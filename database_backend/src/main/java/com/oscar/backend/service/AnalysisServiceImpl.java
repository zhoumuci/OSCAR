package com.oscar.backend.service;

import com.oscar.backend.entity.*;
import com.oscar.backend.entity.SequencePeak2GeneResponse.*;
import com.oscar.backend.mapper.AnalysisMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);
    private static final int MAX_SEQUENCE_FLANK_BP = 1_000_000;
    private static final double NEAR_EQUIVALENT_SCORE_RATIO = 0.95;
    private static final long ENRICHMENT_UNIVERSE_CACHE_TTL_MS = 60_000L;
    private static final double SIGNIFICANT_FDR_THRESHOLD = 0.05;
    private static final Set<String> SUPPORTED_RESULT_LEVELS = Set.of(
            "cell_type",
            "cluster"
    );

    private final AnalysisMapper analysisMapper;
    private final BlastService blastService;
    private final Map<String, CachedUniverseCount> enrichmentUniverseCache = new ConcurrentHashMap<>();

    public AnalysisServiceImpl(AnalysisMapper analysisMapper, BlastService blastService) {
        this.analysisMapper = analysisMapper;
        this.blastService = blastService;
    }

    @Override
    public List<Map<String, String>> getAllDatasetIds() {
        return analysisMapper.selectAllDatasetIds();
    }

    @Override
    public List<String> getCellTypeEnrichmentTissues() {
        MarkerReference markerReference = normalizeMarkerReference("integration_expression");
        return analysisMapper.selectCellTypeEnrichmentTissues(
                markerReference.domain(), markerReference.signalTypes());
    }

    @Override
    public List<Map<String, Object>> getCellTypeEnrichmentDatasets(String tissue) {
        String selectedTissue = requireEnrichmentSelection(tissue, "tissue");
        MarkerReference markerReference = normalizeMarkerReference("integration_expression");
        return analysisMapper.selectCellTypeEnrichmentDatasets(
                selectedTissue, markerReference.domain(), markerReference.signalTypes());
    }

    @Override
    public SequencePeak2GeneResponse sequencePeak2Gene(SequencePeak2GeneRequest req) {
        return sequencePeak2Gene(req, SequenceAnalysisProgressListener.NOOP);
    }

    @Override
    public SequencePeak2GeneResponse sequencePeak2Gene(
            SequencePeak2GeneRequest request,
            SequenceAnalysisProgressListener progressListener
    ) {
        SequencePeak2GeneRequest req = request != null ? request : new SequencePeak2GeneRequest();
        SequenceAnalysisProgressListener progress = progressListener != null
                ? progressListener
                : SequenceAnalysisProgressListener.NOOP;
        int maxTargetSeqs = boundedInt(req.getMaxTargetSeqs(), 500, 1,
                Integer.MAX_VALUE, "maxTargetSeqs");
        int maxHsps = boundedInt(req.getMaxHsps(), 200, 1,
                Integer.MAX_VALUE, "maxHsps");
        int flank = boundedInt(req.getFlankBp(), 0, 0, MAX_SEQUENCE_FLANK_BP, "flankBp");
        int limit = boundedInt(req.getLimit(), 0, 0, Integer.MAX_VALUE, "limit");
        double evalueCutoff = boundedDouble(req.getEvalueCutoff(), 10.0, Double.MIN_VALUE, 10.0,
                "evalueCutoff");
        String blastTask = normalizeBlastTask(req.getBlastTask());
        String resultContent = normalizeSequenceResultContent(req.getResultContent());
        String datasetId = normalizeSequenceDatasetId(req.getReferenceScope(), req.getDatasetId());

        List<BlastHitDto> blastHits;
        String cleaned;
        try {
            progress.update(8, "PREPARING", "Validating the DNA sequence and BLAST parameters.");
            cleaned = blastService.cleanSequence(req.getSequence());

            if ("auto".equals(blastTask) && cleaned.length() > 50) {
                progress.update(18, "BLASTING",
                        "Trying fast hg38 mapping with megablast.");
                blastHits = blastService.runBlast(
                        cleaned,
                        "megablast",
                        maxTargetSeqs,
                        maxHsps,
                        evalueCutoff
                );
                if (blastHits.isEmpty()) {
                    progress.update(45, "BLAST_FALLBACK",
                            "Megablast found no match. Retrying with the more sensitive blastn task; this may take longer.");
                    blastHits = blastService.runBlast(
                            cleaned,
                            "blastn",
                            maxTargetSeqs,
                            maxHsps,
                            evalueCutoff
                    );
                }
            } else {
                String taskMessage = "auto".equals(blastTask)
                        ? "Searching hg38 with blastn-short for the short input sequence."
                        : "Searching hg38 with the selected " + blastTask + " task.";
                progress.update(18, "BLASTING", taskMessage);
                blastHits = blastService.runBlast(
                        cleaned,
                        blastTask,
                        maxTargetSeqs,
                        maxHsps,
                        evalueCutoff
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new RuntimeException("BLAST execution failed: " + e.getMessage(), e);
        }

        progress.update(72, "CLASSIFYING", "Grouping candidate loci and evaluating mapping ambiguity.");
        SequencePeak2GeneResponse resp = new SequencePeak2GeneResponse();

        QueryInfo query = new QueryInfo();
        query.setSequenceLength(cleaned.length());
        query.setGenomeBuild(req.getGenomeBuild() != null ? req.getGenomeBuild() : "hg38");
        query.setUsedHitIndex(0);
        query.setNearEquivalentScoreRatio(NEAR_EQUIVALENT_SCORE_RATIO);
        query.setBlastCoordinateSystem("1-based inclusive");
        query.setEvidenceCoordinateSystem("BED 0-based half-open");
        resp.setQuery(query);

        SummaryInfo summary = new SummaryInfo();
        summary.setBlastHitCount(blastHits.size());

        if (blastHits.isEmpty()) {
            populateEmptySequenceSummary(summary);
            resp.setSummary(summary);
            resp.setMappingStatus("NO_HIT");
            resp.setMappingMessage("No BLAST alignment was found for the input sequence.");
            resp.setBlastHits(List.of());
            resp.setPeakGeneLinks(List.of());
            resp.setMarkerPeaks(List.of());
            progress.update(100, "COMPLETED", "No qualifying hg38 alignment was found.");
            return resp;
        }

        List<BlastHitDto> candidates = distinctCandidateLoci(blastHits);
        MappingDecision mappingDecision = classifyMapping(candidates);
        BlastHitDto selectedHit = candidates.get(0);
        int subjectCount = (int) candidates.stream()
                .map(BlastHitDto::getChromosome)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        boolean perSubjectCapReached = blastHits.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        BlastHitDto::getChromosome,
                        java.util.stream.Collectors.counting()))
                .values().stream().anyMatch(count -> count >= maxHsps);

        summary.setMappedRegionCount(candidates.size());
        summary.setCandidateLocusCount(candidates.size());
        summary.setQualifiedCandidateCount(mappingDecision.qualifiedCount());
        summary.setNearEquivalentLocusCount(mappingDecision.nearEquivalentCount());
        summary.setSubjectCount(subjectCount);
        summary.setCandidateSearchLimited(subjectCount >= maxTargetSeqs || perSubjectCapReached);

        progress.update(84, "QUERYING_EVIDENCE",
                "Retrieving regulatory evidence for the selected BLAST candidate only.");
        SequencePeak2GeneEvidenceResponse evidence = loadSequenceEvidence(
                selectedHit, datasetId, resultContent, flank, limit);
        applyEvidence(resp, summary, evidence);

        resp.setSummary(summary);
        resp.setMappingStatus(mappingDecision.status());
        resp.setMappingMessage(mappingDecision.message());
        resp.setEvidenceHitId(selectedHit.getHitId());
        resp.setBlastHits(candidates);
        summary.setReturnedBlastHitCount(candidates.size());
        summary.setBlastHitsTruncated(false);
        progress.update(100, "COMPLETED", "Sequence mapping and selected-hit evidence retrieval are complete.");
        return resp;
    }

    @Override
    public SequencePeak2GeneEvidenceResponse sequencePeak2GeneEvidence(SequencePeak2GeneEvidenceRequest request) {
        SequencePeak2GeneEvidenceRequest req = request != null
                ? request
                : new SequencePeak2GeneEvidenceRequest();
        if (req.getChromosome() == null || req.getChromosome().isBlank()
                || req.getChromosome().length() > 64) {
            throw new IllegalArgumentException("A valid BLAST chromosome or subject identifier is required.");
        }
        if (req.getStart() == null || req.getEnd() == null
                || req.getStart() < 1L || req.getEnd() < req.getStart()) {
            throw new IllegalArgumentException("BLAST coordinates must be 1-based with end >= start.");
        }

        BlastHitDto hit = new BlastHitDto();
        hit.setHitId(req.getHitId() == null || req.getHitId().isBlank() ? "selected-hit" : req.getHitId());
        hit.setRank(req.getHitRank() != null && req.getHitRank() > 0 ? req.getHitRank() : 1);
        hit.setChromosome(req.getChromosome().trim());
        hit.setStart(req.getStart());
        hit.setEnd(req.getEnd());
        hit.setBedStart(req.getStart() - 1L);
        hit.setBedEnd(req.getEnd());
        hit.setStrand(req.getStrand());

        int flank = boundedInt(req.getFlankBp(), 0, 0, MAX_SEQUENCE_FLANK_BP, "flankBp");
        int limit = boundedInt(req.getLimit(), 0, 0, Integer.MAX_VALUE, "limit");
        String datasetId = normalizeSequenceDatasetId(req.getReferenceScope(), req.getDatasetId());
        String resultContent = normalizeSequenceResultContent(req.getResultContent());
        return loadSequenceEvidence(hit, datasetId, resultContent, flank, limit);
    }

    private SequencePeak2GeneEvidenceResponse loadSequenceEvidence(
            BlastHitDto hit,
            String datasetId,
            String resultContent,
            int flank,
            int limit
    ) {
        long[] bedRegion = toBedRegion(hit, flank);
        List<PeakGeneLinkDto> p2gLinks = List.of();
        List<MarkerPeakDto> markerPeaks = List.of();

        if ("all".equals(resultContent) || "peak_to_gene".equals(resultContent)) {
            p2gLinks = analysisMapper.selectP2gLinksByRegion(
                            hit.getChromosome(), bedRegion[0], bedRegion[1], datasetId, limit)
                    .stream().map(this::mapP2g).toList();
        }
        if ("all".equals(resultContent) || "marker_peaks".equals(resultContent)) {
            markerPeaks = aggregateMarkerPeaks(analysisMapper.selectMarkerPeaksByRegion(
                    hit.getChromosome(), bedRegion[0], bedRegion[1], datasetId, limit));
        }

        Set<String> overlappingPeaks = new HashSet<>();
        p2gLinks.forEach(row -> overlappingPeaks.add(peakCoordinateKey(
                row.getChromosome(), row.getPeakStart(), row.getPeakEnd())));
        markerPeaks.forEach(row -> overlappingPeaks.add(peakCoordinateKey(
                row.getChromosome(), row.getPeakStart(), row.getPeakEnd())));

        SequencePeak2GeneEvidenceResponse response = new SequencePeak2GeneEvidenceResponse();
        response.setHitId(hit.getHitId());
        response.setHitRank(hit.getRank());
        response.setChromosome(hit.getChromosome());
        response.setStart(hit.getStart());
        response.setEnd(hit.getEnd());
        response.setBedStart(bedRegion[0]);
        response.setBedEnd(bedRegion[1]);
        response.setOverlappingPeakCount(overlappingPeaks.size());
        Set<String> linkedGenes = new HashSet<>();
        p2gLinks.stream()
                .map(PeakGeneLinkDto::getGeneName)
                .filter(Objects::nonNull)
                .forEach(linkedGenes::add);
        markerPeaks.stream()
                .map(MarkerPeakDto::getPeakGeneLinks)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(PeakGeneLinkDto::getGeneName)
                .filter(Objects::nonNull)
                .forEach(linkedGenes::add);
        response.setLinkedGeneCount(linkedGenes.size());
        response.setReturnedP2gCount(p2gLinks.size());
        response.setReturnedMarkerPeakCount(markerPeaks.size());
        response.setPossiblyTruncated(limit > 0
                && (p2gLinks.size() >= limit || markerPeaks.size() >= limit));
        response.setPeakGeneLinks(p2gLinks);
        response.setMarkerPeaks(markerPeaks);
        return response;
    }

    private static void applyEvidence(
            SequencePeak2GeneResponse response,
            SummaryInfo summary,
            SequencePeak2GeneEvidenceResponse evidence
    ) {
        response.setEvidenceHitId(evidence.getHitId());
        response.setPeakGeneLinks(evidence.getPeakGeneLinks());
        response.setMarkerPeaks(evidence.getMarkerPeaks());
        summary.setOverlappingPeakCount(evidence.getOverlappingPeakCount());
        summary.setLinkedGeneCount(evidence.getLinkedGeneCount());
        summary.setMarkerPeakCount(evidence.getReturnedMarkerPeakCount());
        summary.setReturnedP2gCount(evidence.getReturnedP2gCount());
        summary.setReturnedMarkerPeakCount(evidence.getReturnedMarkerPeakCount());
        summary.setEvidencePossiblyTruncated(evidence.isPossiblyTruncated());
    }

    static List<BlastHitDto> distinctCandidateLoci(List<BlastHitDto> hits) {
        Comparator<BlastHitDto> scientificRank = Comparator
                .comparingDouble(BlastHitDto::getBitScore).reversed()
                .thenComparingDouble(hit -> parseSequenceEvalue(hit.getEvalue()))
                .thenComparing(Comparator.comparingDouble(BlastHitDto::getQueryCoverage).reversed())
                .thenComparing(Comparator.comparingDouble(BlastHitDto::getIdentity).reversed())
                .thenComparing(hit -> Objects.toString(hit.getChromosome(), ""))
                .thenComparingLong(BlastHitDto::getStart)
                .thenComparingLong(BlastHitDto::getEnd);

        // BLAST can emit several overlapping HSPs for one physical genomic
        // location.  They are alignment fragments, not independent mapping
        // alternatives, so collapse each overlap-connected component and keep
        // its strongest representative. Opposite strands at the same physical
        // coordinates also remain one locus.
        List<BlastHitDto> coordinateOrder = new ArrayList<>(hits);
        coordinateOrder.sort(Comparator
                .comparing((BlastHitDto hit) -> Objects.toString(hit.getChromosome(), ""))
                .thenComparingLong(BlastHitDto::getStart)
                .thenComparingLong(BlastHitDto::getEnd));
        List<BlastHitDto> candidates = new ArrayList<>();
        List<BlastHitDto> locus = new ArrayList<>();
        String locusSubject = null;
        long locusEnd = Long.MIN_VALUE;
        for (BlastHitDto hit : coordinateOrder) {
            String subject = Objects.toString(hit.getChromosome(), "");
            if (!locus.isEmpty() && (!subject.equals(locusSubject) || hit.getStart() > locusEnd)) {
                candidates.add(locus.stream().min(scientificRank).orElseThrow());
                locus.clear();
                locusEnd = Long.MIN_VALUE;
            }
            if (locus.isEmpty()) {
                locusSubject = subject;
            }
            locus.add(hit);
            locusEnd = Math.max(locusEnd, hit.getEnd());
        }
        if (!locus.isEmpty()) {
            candidates.add(locus.stream().min(scientificRank).orElseThrow());
        }

        candidates.sort(scientificRank);

        double topScore = candidates.isEmpty() ? 0.0 : candidates.get(0).getBitScore();
        for (int i = 0; i < candidates.size(); i++) {
            BlastHitDto hit = candidates.get(i);
            hit.setRank(i + 1);
            hit.setHitId("hit-" + (i + 1));
            hit.setPrimary(i == 0);
            hit.setBedStart(Math.max(0L, hit.getStart() - 1L));
            hit.setBedEnd(hit.getEnd());
            hit.setScoreRatio(topScore > 0.0 ? hit.getBitScore() / topScore : 0.0);
            hit.setNearEquivalent(i > 0
                    && hit.getScoreRatio() >= NEAR_EQUIVALENT_SCORE_RATIO);
        }
        return candidates;
    }

    static MappingDecision classifyMapping(List<BlastHitDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new MappingDecision("NO_HIT", "No hg38 alignment was found.", 0, 0);
        }
        int total = candidates.size();
        int nearEquivalent = (int) candidates.stream().filter(BlastHitDto::isNearEquivalent).count();
        if (nearEquivalent > 0) {
            return new MappingDecision(
                    "AMBIGUOUS",
                    "Near-equivalent alignments (at least 95% of the top bit score) exist at distinct genomic loci. Regulatory evidence is shown only for the selected candidate and is not merged across loci.",
                    total,
                    nearEquivalent
            );
        }
        if (total > 1) {
            return new MappingDecision(
                    "BEST_SUPPORTED",
                    "Multiple loci were found, but the selected candidate has stronger BLAST support than the alternatives.",
                    total,
                    0
            );
        }
        return new MappingDecision(
                "UNIQUE",
                "One candidate locus was found.",
                1,
                0
        );
    }

    static long[] toBedRegion(BlastHitDto hit, int flank) {
        long bedStart = Math.max(0L, hit.getStart() - 1L - flank);
        long bedEnd;
        try {
            bedEnd = Math.addExact(hit.getEnd(), flank);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("BLAST region exceeds the supported coordinate range.");
        }
        return new long[]{bedStart, bedEnd};
    }

    private static String peakCoordinateKey(String chromosome, long start, long end) {
        return Objects.toString(chromosome, "") + ":" + start + "-" + end;
    }

    private static double parseSequenceEvalue(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException e) { return Double.MAX_VALUE; }
    }

    private static int boundedInt(Integer value, int defaultValue, int min, int max, String field) {
        int normalized = value != null ? value : defaultValue;
        if (normalized < min || normalized > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max + ".");
        }
        return normalized;
    }

    private static double boundedDouble(Double value, double defaultValue, double min, double max, String field) {
        double normalized = value != null ? value : defaultValue;
        if (!Double.isFinite(normalized) || normalized < min || normalized > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max + ".");
        }
        return normalized;
    }

    private static String normalizeBlastTask(String value) {
        String normalized = value == null || value.isBlank() ? "auto" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("auto", "megablast", "blastn", "blastn-short").contains(normalized)) {
            throw new IllegalArgumentException("blastTask must be auto, megablast, blastn, or blastn-short.");
        }
        return normalized;
    }

    private static String normalizeSequenceResultContent(String value) {
        String normalized = value == null || value.isBlank() ? "all" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("all", "peak_to_gene", "marker_peaks").contains(normalized)) {
            throw new IllegalArgumentException("resultContent must be all, peak_to_gene, or marker_peaks.");
        }
        return normalized;
    }

    private static String normalizeSequenceDatasetId(String referenceScope, String datasetId) {
        if (!"single_dataset".equals(referenceScope)) {
            return null;
        }
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("datasetId is required when referenceScope is single_dataset.");
        }
        return datasetId.trim();
    }

    private static void populateEmptySequenceSummary(SummaryInfo summary) {
        summary.setReturnedBlastHitCount(0);
        summary.setBlastHitsTruncated(false);
        summary.setMappedRegionCount(0);
        summary.setCandidateLocusCount(0);
        summary.setQualifiedCandidateCount(0);
        summary.setNearEquivalentLocusCount(0);
        summary.setSubjectCount(0);
        summary.setCandidateSearchLimited(false);
        summary.setOverlappingPeakCount(0);
        summary.setLinkedGeneCount(0);
        summary.setMarkerPeakCount(0);
        summary.setReturnedP2gCount(0);
        summary.setReturnedMarkerPeakCount(0);
        summary.setEvidencePossiblyTruncated(false);
    }

    record MappingDecision(String status, String message, int qualifiedCount, int nearEquivalentCount) { }

    @Override
    public CellTypeEnrichmentResponse cellTypeEnrichment(CellTypeEnrichmentRequest request) {
        long startedAt = System.currentTimeMillis();
        CellTypeEnrichmentRequest req = request != null ? request : new CellTypeEnrichmentRequest();
        List<String> inputGenes = normalizeGeneSymbols(req.getGeneSymbols());
        String resultLevel = normalizeResultLevel(req.getResultLevel());
        MarkerReference markerReference = normalizeMarkerReference(req.getMarkerReference());
        String tissue = requireEnrichmentSelection(req.getTissue(), "tissue");
        String datasetId = requireEnrichmentSelection(req.getDatasetId(), "datasetId");
        if (analysisMapper.countCellTypeEnrichmentDatasetInTissue(
                tissue, datasetId, markerReference.domain(), markerReference.signalTypes()) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected dataset is not available for cell type enrichment in the selected tissue."
            );
        }
        int minOverlap = normalizeMinOverlap(req.getMinOverlap());
        normalizeBackgroundUniverse(req.getBackgroundUniverse());
        normalizeFdrMethod(req.getFdrMethod());

        CellTypeEnrichmentResponse response = new CellTypeEnrichmentResponse();
        response.setInputGenes(inputGenes);
        if (inputGenes.isEmpty()) {
            return response;
        }

        String referenceCacheKey = enrichmentReferenceCacheKey(markerReference, datasetId);
        long universeLong = enrichmentUniverseCount(referenceCacheKey, markerReference, datasetId);
        if (universeLong <= 0) {
            response.setUnmatchedGenes(inputGenes);
            return response;
        }
        if (universeLong > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Marker gene universe is too large for enrichment calculation."
            );
        }
        int universeSize = (int) universeLong;

        Map<String, Integer> inputOrder = new HashMap<>();
        for (int i = 0; i < inputGenes.size(); i++) {
            inputOrder.put(inputGenes.get(i), i);
        }

        Set<String> matchedSet = new HashSet<>();
        Set<String> candidateCellTypes = new LinkedHashSet<>();
        Set<String> candidateContexts = new LinkedHashSet<>();
        Map<String, List<String>> overlapGenesBySet = new HashMap<>();
        List<Map<String, Object>> overlapRows = analysisMapper.selectEnrichmentOverlapGenes(
                inputGenes, resultLevel, markerReference.domain(), markerReference.signalTypes(), datasetId);
        for (Map<String, Object> row : overlapRows) {
            String gene = str(value(row, "geneSymbol", "gene_symbol"));
            if (gene == null || gene.isBlank()) {
                continue;
            }
            matchedSet.add(gene);
            addCandidateValue(candidateCellTypes, value(row, "cellType", "cell_type"));
            addCandidateValue(candidateContexts, value(row, "context"));
            overlapGenesBySet.computeIfAbsent(enrichmentKey(row, resultLevel), ignored -> new ArrayList<>()).add(gene);
        }
        List<String> matchedGenes = inputGenes.stream().filter(matchedSet::contains).toList();
        List<String> unmatchedGenes = inputGenes.stream().filter(gene -> !matchedSet.contains(gene)).toList();
        response.setMatchedGenes(matchedGenes);
        response.setUnmatchedGenes(unmatchedGenes);
        if (matchedGenes.isEmpty()) {
            return response;
        }
        for (List<String> genes : overlapGenesBySet.values()) {
            genes.sort(Comparator.comparingInt(gene -> inputOrder.getOrDefault(gene, Integer.MAX_VALUE)));
        }

        List<Map<String, Object>> markerSetRows = analysisMapper.selectCandidateEnrichmentMarkerSets(
                resultLevel,
                markerReference.domain(),
                markerReference.signalTypes(),
                datasetId,
                List.copyOf(candidateCellTypes),
                List.copyOf(candidateContexts)
        );
        if (markerSetRows.isEmpty()) {
            return response;
        }

        double[] logFactorials = buildLogFactorials(universeSize);
        int drawCount = matchedGenes.size();
        List<ComputedEnrichmentSet> computedSets = new ArrayList<>(markerSetRows.size());
        for (Map<String, Object> row : markerSetRows) {
            String key = enrichmentKey(row, resultLevel);
            String cellType = displayValue(value(row, "cellType", "cell_type"), "Unknown");
            String context = displayValue(value(row, "context"), cellType);
            int setSize = intValue(value(row, "setSize", "set_size"));
            int datasetCount = intValue(value(row, "datasetCount", "dataset_count"));
            if (setSize <= 0) {
                continue;
            }

            List<String> genes = overlapGenesBySet.getOrDefault(key, List.of());
            int overlap = genes.size();
            if (overlap <= 0) {
                continue;
            }
            double pValue = overlap > 0
                    ? hypergeometricRightTail(universeSize, setSize, drawCount, overlap, logFactorials)
                    : 1.0;
            double expected = expectedOverlap(universeSize, setSize, drawCount);
            double enrichmentFold = expected > 0.0
                    ? overlap / expected
                    : 0.0;
            computedSets.add(new ComputedEnrichmentSet(
                    cellType, context, setSize, datasetCount, genes, overlap, enrichmentFold, pValue));
        }

        List<ComputedEnrichmentSet> normalizedSets = computedSets;

        long completeHypothesisCount = analysisMapper.countEnrichmentMarkerSets(
                resultLevel,
                markerReference.domain(),
                markerReference.signalTypes(),
                datasetId
        );
        long totalMarkerSets = Math.max(completeHypothesisCount, normalizedSets.size());
        applyBenjaminiHochberg(normalizedSets, totalMarkerSets);

        List<ComputedEnrichmentSet> filtered = normalizedSets.stream()
                .filter(row -> row.overlap >= minOverlap)
                .sorted(Comparator
                        .comparingDouble((ComputedEnrichmentSet row) -> row.fdr)
                        .thenComparingDouble(row -> row.pValue)
                        .thenComparing((ComputedEnrichmentSet row) -> row.enrichmentFold, Comparator.reverseOrder())
                        .thenComparing((ComputedEnrichmentSet row) -> row.overlap, Comparator.reverseOrder())
                        .thenComparing(row -> row.cellType)
                        .thenComparing(row -> row.context))
                .toList();

        List<CellTypeEnrichmentResponse.EnrichmentResultRow> resultRows = new ArrayList<>(filtered.size());
        for (int i = 0; i < filtered.size(); i++) {
            ComputedEnrichmentSet result = filtered.get(i);
            CellTypeEnrichmentResponse.EnrichmentResultRow row = new CellTypeEnrichmentResponse.EnrichmentResultRow();
            row.setRank(i + 1);
            row.setCellType(result.cellType);
            row.setContext(result.context);
            row.setOverlap(result.overlap);
            row.setEnrichmentFold(result.enrichmentFold);
            row.setPValue(result.pValue);
            row.setFdr(result.fdr);
            row.setDatasetCount(result.datasetCount);
            row.setGenes(result.genes);
            resultRows.add(row);
        }

        response.setResults(resultRows);
        response.setTotalResults(resultRows.size());
        response.setSignificantResults((int) resultRows.stream()
                .filter(row -> row.getFdr() <= SIGNIFICANT_FDR_THRESHOLD)
                .count());
        response.setTopEnrichedCellType(resultRows.isEmpty() ? null : resultRows.get(0).getCellType());
        log.info("Cell enrichment: {} input genes, {} matched, {} candidate marker sets, {} total tests, {} returned, {} ms",
                inputGenes.size(), matchedGenes.size(), normalizedSets.size(), totalMarkerSets, resultRows.size(),
                System.currentTimeMillis() - startedAt);
        return response;
    }

    private PeakGeneLinkDto mapP2g(Map<String, Object> row) {
        PeakGeneLinkDto d = new PeakGeneLinkDto();
        d.setDatasetId(str(row.get("dataset_id")));
        d.setDomain(str(row.get("domain")));
        d.setPeakName(str(row.get("peak_name")));
        d.setChromosome(str(row.get("chromosome")));
        d.setPeakStart(toLong(row.get("peak_start")));
        d.setPeakEnd(toLong(row.get("peak_end")));
        d.setGeneName(str(row.get("gene_name")));
        d.setCorrelation(toDouble(row.get("correlation")));
        d.setFdr(toDouble(row.get("fdr")));
        d.setLinkScore(toDouble(row.get("link_score")));
        d.setSourceFile(str(row.get("source_file")));
        return d;
    }

    private MarkerPeakDto mapMarkerPeak(Map<String, Object> row) {
        MarkerPeakDto d = new MarkerPeakDto();
        d.setDatasetId(str(row.get("dataset_id")));
        d.setDomain(str(row.get("domain")));
        d.setClusterSource(str(row.get("cluster_source")));
        d.setGroupName(str(row.get("group_name")));
        d.setPeakName(str(row.get("peak_name")));
        d.setChromosome(str(row.get("chromosome")));
        d.setPeakStart(toLong(row.get("peak_start")));
        d.setPeakEnd(toLong(row.get("peak_end")));
        d.setLog2fc(toDouble(row.get("log2fc")));
        d.setFdr(toDouble(row.get("fdr")));
        d.setMeanDiff(toDouble(row.get("mean_diff")));
        d.setSourceFile(str(row.get("source_file")));
        d.setPeakGeneLinks(new ArrayList<>());
        return d;
    }

    private List<MarkerPeakDto> aggregateMarkerPeaks(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, MarkerPeakDto> markersById = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            long markerPeakId = toLong(value(row, "marker_peak_id", "markerPeakId"));
            MarkerPeakDto markerPeak = markersById.computeIfAbsent(markerPeakId, ignored -> mapMarkerPeak(row));
            String geneName = str(value(row, "p2g_gene_name", "p2gGeneName"));
            if (geneName == null || geneName.isBlank()) {
                continue;
            }
            PeakGeneLinkDto link = new PeakGeneLinkDto();
            link.setDatasetId(markerPeak.getDatasetId());
            link.setDomain(markerPeak.getDomain());
            link.setPeakName(str(value(row, "p2g_peak_name", "p2gPeakName")));
            link.setChromosome(str(value(row, "p2g_chromosome", "p2gChromosome")));
            link.setPeakStart(toLong(value(row, "p2g_peak_start", "p2gPeakStart")));
            link.setPeakEnd(toLong(value(row, "p2g_peak_end", "p2gPeakEnd")));
            link.setGeneName(geneName);
            link.setCorrelation(toDouble(value(row, "p2g_correlation", "p2gCorrelation")));
            link.setFdr(toDouble(value(row, "p2g_fdr", "p2gFdr")));
            link.setLinkScore(toDouble(value(row, "p2g_link_score", "p2gLinkScore")));
            link.setSourceFile(str(value(row, "p2g_source_file", "p2gSourceFile")));
            markerPeak.getPeakGeneLinks().add(link);
        }
        return new ArrayList<>(markersById.values());
    }

    private static String str(Object v) { return v == null ? null : v.toString(); }

    private static Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static int intValue(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static void addCandidateValue(Set<String> values, Object value) {
        String rawValue = str(value);
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        values.add(rawValue);
    }

    private static List<String> normalizeGeneSymbols(List<String> rawGenes) {
        if (rawGenes == null || rawGenes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawGene : rawGenes) {
            String gene = rawGene == null ? "" : rawGene.trim().toUpperCase(Locale.ROOT);
            if (gene.isEmpty()) {
                continue;
            }
            if (!isValidGeneSymbol(gene)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gene symbol: " + rawGene);
            }
            normalized.add(gene);
        }
        return List.copyOf(normalized);
    }

    private static boolean isValidGeneSymbol(String gene) {
        if (gene.length() > 30) {
            return false;
        }
        char first = gene.charAt(0);
        if (first < 'A' || first > 'Z') {
            return false;
        }
        for (int i = 1; i < gene.length(); i++) {
            char ch = gene.charAt(i);
            boolean ok = (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '.'
                    || ch == '-'
                    || ch == '@';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeResultLevel(String resultLevel) {
        String value = resultLevel == null || resultLevel.isBlank()
                ? "cell_type"
                : resultLevel.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_RESULT_LEVELS.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resultLevel: " + resultLevel);
        }
        return value;
    }

    private static String requireEnrichmentSelection(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return normalized;
    }

    private static MarkerReference normalizeMarkerReference(String markerReference) {
        String value = markerReference == null || markerReference.isBlank()
                ? "integration_expression"
                : markerReference.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "integration_expression" ->
                    new MarkerReference("integration_expression", "integration", List.of("gene_expression", "gene_exp", "gene_score", ""));
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only integration_expression markerReference is supported."
            );
        };
    }

    private static int normalizeMinOverlap(Integer minOverlap) {
        int value = minOverlap == null ? 1 : minOverlap;
        if (value < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minOverlap must be at least 1.");
        }
        return value;
    }

    private static void normalizeBackgroundUniverse(String backgroundUniverse) {
        String value = backgroundUniverse == null || backgroundUniverse.isBlank()
                ? "selected_marker_reference"
                : backgroundUniverse.trim().toLowerCase(Locale.ROOT);
        if (!"selected_marker_reference".equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only selected_marker_reference backgroundUniverse is supported."
            );
        }
    }

    private static void normalizeFdrMethod(String fdrMethod) {
        String value = fdrMethod == null || fdrMethod.isBlank() ? "BH" : fdrMethod.trim();
        if (!"BH".equalsIgnoreCase(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only BH FDR correction is supported.");
        }
    }

    private static String enrichmentReferenceCacheKey(MarkerReference markerReference, String datasetId) {
        return markerReference.domain()
                + "|"
                + String.join(",", markerReference.signalTypes())
                + "|"
                + (datasetId == null ? "*" : datasetId);
    }

    private long enrichmentUniverseCount(String cacheKey, MarkerReference markerReference, String datasetId) {
        long now = System.currentTimeMillis();
        CachedUniverseCount cached = enrichmentUniverseCache.get(cacheKey);
        if (cached != null && now - cached.cachedAtMs <= ENRICHMENT_UNIVERSE_CACHE_TTL_MS) {
            return cached.value;
        }
        Long globalStatsUniverse = datasetId == null
                ? analysisMapper.selectGlobalEnrichmentUniverseSize(markerReference.key())
                : null;
        long count = globalStatsUniverse != null && globalStatsUniverse > 0
                ? globalStatsUniverse
                : analysisMapper.countEnrichmentUniverseGenes(
                        markerReference.domain(), markerReference.signalTypes(), datasetId);
        enrichmentUniverseCache.put(cacheKey, new CachedUniverseCount(count, now));
        return count;
    }

    private static String enrichmentKey(Map<String, Object> row, String resultLevel) {
        String cellType = displayValue(value(row, "cellType", "cell_type"), "Unknown");
        String context = displayValue(value(row, "context"), cellType);
        if ("cluster".equals(resultLevel)) {
            return context;
        }
        return cellType
                + "\u001F"
                + context;
    }

    private static String displayValue(Object value, String fallback) {
        String text = str(value);
        if (text == null) {
            return fallback;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static double expectedOverlap(int populationSize, int successStates, int draws) {
        if (populationSize <= 0) {
            return 0.0;
        }
        return (double) draws * (double) successStates / (double) populationSize;
    }

    static double[] buildLogFactorials(int max) {
        double[] logFactorials = new double[max + 1];
        for (int i = 2; i <= max; i++) {
            logFactorials[i] = logFactorials[i - 1] + Math.log(i);
        }
        return logFactorials;
    }

    static double hypergeometricRightTail(
            int populationSize,
            int successStates,
            int draws,
            int observed,
            double[] logFactorials
    ) {
        int upper = Math.min(successStates, draws);
        int lower = Math.max(0, draws - (populationSize - successStates));
        if (observed <= lower) {
            return 1.0;
        }
        if (observed > upper) {
            return 0.0;
        }

        double logDenominator = logCombination(populationSize, draws, logFactorials);
        double logSum = Double.NEGATIVE_INFINITY;
        for (int i = observed; i <= upper; i++) {
            double logProbability = logCombination(successStates, i, logFactorials)
                    + logCombination(populationSize - successStates, draws - i, logFactorials)
                    - logDenominator;
            logSum = logAdd(logSum, logProbability);
        }
        if (Double.isInfinite(logSum)) {
            return 0.0;
        }
        if (logSum >= 0.0) {
            return 1.0;
        }
        double minLog = Math.log(Double.MIN_VALUE);
        return logSum < minLog ? Double.MIN_VALUE : Math.min(1.0, Math.exp(logSum));
    }

    private static double logCombination(int n, int k, double[] logFactorials) {
        if (k < 0 || k > n) {
            return Double.NEGATIVE_INFINITY;
        }
        return logFactorials[n] - logFactorials[k] - logFactorials[n - k];
    }

    private static double logAdd(double logA, double logB) {
        if (Double.isInfinite(logA)) {
            return logB;
        }
        if (Double.isInfinite(logB)) {
            return logA;
        }
        double max = Math.max(logA, logB);
        double min = Math.min(logA, logB);
        return max + Math.log1p(Math.exp(min - max));
    }

    private static void applyBenjaminiHochberg(List<ComputedEnrichmentSet> rows, long totalTests) {
        double[] pValues = rows.stream().mapToDouble(row -> row.pValue).toArray();
        double[] adjusted = benjaminiHochbergAdjusted(pValues, totalTests);
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).fdr = adjusted[i];
        }
    }

    static double[] benjaminiHochbergAdjusted(double[] pValues, long totalTests) {
        Integer[] order = new Integer[pValues.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(index -> pValues[index]));

        long tests = Math.max(totalTests, pValues.length);
        double[] adjusted = new double[pValues.length];
        double next = 1.0;
        for (int rankIndex = order.length - 1; rankIndex >= 0; rankIndex--) {
            int originalIndex = order[rankIndex];
            double rankAdjusted = pValues[originalIndex] * tests / (rankIndex + 1.0);
            next = Math.min(next, Math.min(1.0, rankAdjusted));
            adjusted[originalIndex] = next;
        }
        return adjusted;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private record MarkerReference(String key, String domain, List<String> signalTypes) {}

    private record CachedUniverseCount(long value, long cachedAtMs) {}

    private static class ComputedEnrichmentSet {
        private final String cellType;
        private final String context;
        private final int setSize;
        private final int datasetCount;
        private final List<String> genes;
        private final int overlap;
        private final double enrichmentFold;
        private final double pValue;
        private double fdr = 1.0;

        private ComputedEnrichmentSet(
                String cellType,
                String context,
                int setSize,
                int datasetCount,
                List<String> genes,
                int overlap,
                double enrichmentFold,
                double pValue
        ) {
            this.cellType = cellType;
            this.context = context;
            this.setSize = setSize;
            this.datasetCount = datasetCount;
            this.genes = List.copyOf(genes);
            this.overlap = overlap;
            this.enrichmentFold = enrichmentFold;
            this.pValue = pValue;
        }
    }

    private static long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v == null) return 0L;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0L; }
    }
    private static Double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v == null) return null;
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    @Override
    public List<Map<String, Object>> getSamplesByCellType(String cellType) {
        return analysisMapper.selectSamplesByCellType(cellType);
    }

    @Override
    public List<Map<String, Object>> getSamplesByTissue(String tissue) {
        return analysisMapper.selectSamplesByTissue(tissue);
    }
}
