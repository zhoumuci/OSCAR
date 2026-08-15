package com.oscar.backend.service;

import com.oscar.backend.entity.PeakGeneContextRequest;
import com.oscar.backend.entity.PeakGeneContextResponse;
import com.oscar.backend.mapper.PeakGeneContextMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PeakGeneContextService {

    private static final Logger log = LoggerFactory.getLogger(PeakGeneContextService.class);
    private static final Pattern GENE_SYMBOL = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,127}$");
    private static final Pattern CHROMOSOME = Pattern.compile("^chr[A-Za-z0-9_.-]+$");
    private static final Set<String> REFERENCE_MODES = Set.of("p2g_only", "p2g_markers");
    private static final Set<String> RESULT_TYPES = Set.of("general", "cell_type");
    private static final int NETWORK_PEAK_LIMIT_PER_GENE = 30;

    private final PeakGeneContextMapper mapper;
    private final PeakGeneContextBedtoolsService bedtoolsService;

    public PeakGeneContextService(
            PeakGeneContextMapper mapper,
            PeakGeneContextBedtoolsService bedtoolsService
    ) {
        this.mapper = mapper;
        this.bedtoolsService = bedtoolsService;
    }

    public List<String> getTissues() {
        return mapper.selectP2gTissues();
    }

    public List<Map<String, Object>> getDatasets(String tissue) {
        return mapper.selectP2gDatasets(requireText(tissue, "tissue"));
    }

    public PeakGeneContextResponse analyze(PeakGeneContextRequest request) {
        return analyze(request, PeakGeneContextProgressListener.NOOP);
    }

    public PeakGeneContextResponse analyze(
            PeakGeneContextRequest request,
            PeakGeneContextProgressListener progressListener
    ) {
        PeakGeneContextProgressListener progress = progressListener;
        progress.update(8, "VALIDATING", "Validating peak, gene, tissue, dataset, and result selections.");
        ValidatedRequest validated = validate(request);

        long startedAt = System.currentTimeMillis();
        progress.update(22, "QUERYING_CANDIDATES", "Loading indexed Peak-to-Gene candidates for the selected tissue, dataset, and genes.");
        List<Map<String, Object>> candidates = "p2g_only".equals(validated.referenceMode())
                ? mapper.selectRawCandidates(validated.genes(), validated.tissue(), validated.datasetId())
                : mapper.selectMarkerCandidates(
                        validated.genes(),
                        validated.tissue(),
                        validated.datasetId(),
                        "cell_type".equals(validated.resultType())
                );

        progress.update(45, "INTERSECTING_REGIONS", "Running bedtools intersect against the submitted peak regions.");
        List<Map<String, Object>> matchedRows = bedtoolsService.intersect(
                validated.peaks(),
                candidates,
                validated.minOverlapBp()
        );
        if ("general".equals(validated.resultType())) {
            matchedRows.sort(pairComparator());
            matchedRows = collapseGeneralRows(matchedRows, validated.referenceMode());
        } else {
            matchedRows.sort(cellTypeRowComparator(matchedRows));
        }

        int returnedRowCount = validated.maxReturnedLinks() == null
                ? matchedRows.size()
                : Math.min(matchedRows.size(), validated.maxReturnedLinks());
        List<Map<String, Object>> returnedRows = new ArrayList<>(matchedRows.subList(0, returnedRowCount));

        progress.update(64, "ANNOTATING_MARKERS", "Adding marker-peak and marker-gene labels to the returned links.");
        if ("p2g_only".equals(validated.referenceMode())) {
            annotateRawRows(returnedRows);
        } else if ("cell_type".equals(validated.resultType())) {
            annotateMarkerRows(returnedRows);
        }

        List<PeakGeneContextResponse.CellTypeResult> cellTypeResults = List.of();
        if ("cell_type".equals(validated.resultType())) {
            progress.update(78, "AGGREGATING_CELL_TYPES", "Counting every matched context record and grouping the matches by displayed cell type.");
            cellTypeResults = buildCellTypeResults(matchedRows);
            sortCellTypeResults(cellTypeResults);
        }

        progress.update(91, "BUILDING_RESULTS", "Building the result table, marker labels, summaries, and Peak-to-Gene network.");
        PeakGeneContextResponse response = buildResponse(validated, matchedRows, returnedRows, cellTypeResults);

        log.info(
                "PeakGeneContext completed in {} ms (tissue={}, dataset={}, referenceMode={}, resultType={}, candidates={}, matched={}, returned={})",
                System.currentTimeMillis() - startedAt,
                validated.tissue(),
                validated.datasetId(),
                validated.referenceMode(),
                validated.resultType(),
                candidates.size(),
                matchedRows.size(),
                returnedRows.size()
        );
        return response;
    }

    private ValidatedRequest validate(PeakGeneContextRequest request) {
        if (request == null) {
            throw badRequest("Request body is required.");
        }
        if (request.getPeaks() == null || request.getPeaks().isEmpty()) {
            throw badRequest("Peak set is required.");
        }
        List<Map<String, Object>> peaks = new ArrayList<>(request.getPeaks().size());
        Set<String> peakKeys = new LinkedHashSet<>();
        for (PeakGeneContextRequest.PeakInput peak : request.getPeaks()) {
            if (peak == null || peak.getChrom() == null || !CHROMOSOME.matcher(peak.getChrom().trim()).matches()) {
                throw badRequest("Every peak must use chr:start-end coordinates.");
            }
            if (peak.getStart() < 0 || peak.getStart() >= peak.getEnd()) {
                throw badRequest("Every peak must satisfy 0 <= start < end.");
            }
            String chrom = peak.getChrom().trim();
            String key = chrom + ':' + peak.getStart() + '-' + peak.getEnd();
            if (!peakKeys.add(key)) {
                throw badRequest("Peak set contains duplicate regions.");
            }
            Map<String, Object> values = new HashMap<>();
            values.put("chrom", chrom);
            values.put("start", peak.getStart());
            values.put("end", peak.getEnd());
            peaks.add(values);
        }

        if (request.getGenes() == null || request.getGenes().isEmpty()) {
            throw badRequest("Gene set is required.");
        }
        List<String> genes = new ArrayList<>(request.getGenes().size());
        Set<String> geneKeys = new LinkedHashSet<>();
        for (String value : request.getGenes()) {
            String gene = requireText(value, "gene").toUpperCase(Locale.ROOT);
            if (!GENE_SYMBOL.matcher(gene).matches()) {
                throw badRequest("Gene set contains an invalid gene symbol: " + value);
            }
            if (!geneKeys.add(gene)) {
                throw badRequest("Gene set contains duplicate symbols.");
            }
            genes.add(gene);
        }

        String tissue = requireText(request.getTissue(), "tissue");
        List<Map<String, Object>> tissueDatasets = mapper.selectP2gDatasets(tissue);
        if (tissueDatasets.isEmpty()) {
            throw badRequest("Selected tissue has no Integration Peak-to-Gene datasets.");
        }

        String datasetId = nullableText(request.getDatasetId());
        if (datasetId != null && mapper.countP2gDatasetInTissue(tissue, datasetId) != 1) {
            throw badRequest("Selected dataset does not belong to the selected tissue or has no Integration Peak-to-Gene data.");
        }

        String referenceMode = requireText(request.getReferenceMode(), "referenceMode");
        if (!REFERENCE_MODES.contains(referenceMode)) {
            throw badRequest("Unsupported referenceMode: " + referenceMode);
        }
        String resultType = requireText(request.getResultType(), "resultType");
        if (!RESULT_TYPES.contains(resultType)) {
            throw badRequest("Unsupported resultType: " + resultType);
        }
        if ("p2g_only".equals(referenceMode) && "cell_type".equals(resultType)) {
            throw badRequest("Cell type results require Peak-to-Gene links + marker reference mode.");
        }

        PeakGeneContextRequest.Advanced advanced = request.getAdvanced();
        if (advanced == null) {
            throw badRequest("Advanced query settings are required.");
        }
        if (advanced.getMinOverlapBp() < 1) {
            throw badRequest("minOverlapBp must be at least 1.");
        }
        if (advanced.getMaxReturnedLinks() != null && advanced.getMaxReturnedLinks() < 1) {
            throw badRequest("maxReturnedLinks must be at least 1 when provided.");
        }
        return new ValidatedRequest(
                peaks,
                genes,
                tissue,
                datasetId,
                referenceMode,
                resultType,
                advanced.getMinOverlapBp(),
                advanced.getMaxReturnedLinks()
        );
    }

    private Comparator<Map<String, Object>> pairComparator() {
        return Comparator.<Map<String, Object>>comparingDouble(row -> doubleValue(row.get("link_score")))
                .reversed()
                .thenComparing(
                        row -> nullableDouble(row.get("link_fdr")),
                        Comparator.nullsLast(Double::compareTo)
                )
                .thenComparingLong(row -> longValue(row.get("peak_start")))
                .thenComparingLong(row -> longValue(row.get("evidence_id")));
    }

    private Comparator<Map<String, Object>> cellTypeRowComparator(List<Map<String, Object>> rows) {
        Map<CellTypeLinkKey, Integer> matchCounts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            matchCounts.merge(cellTypeLinkKey(row), 1, Integer::sum);
        }
        return Comparator.<Map<String, Object>>comparingInt(row -> matchCounts.get(cellTypeLinkKey(row)))
                .reversed()
                .thenComparing(row -> stringValue(row.get("cell_type")))
                .thenComparing(row -> stringValue(row.get("gene_name")))
                .thenComparing(row -> stringValue(row.get("peak_name")))
                .thenComparing(row -> stringValue(row.get("dataset_id")))
                .thenComparingLong(row -> longValue(row.get("evidence_id")));
    }

    private CellTypeLinkKey cellTypeLinkKey(Map<String, Object> row) {
        return new CellTypeLinkKey(
                stringValue(row.get("cell_type")),
                stringValue(row.get("peak_name")),
                stringValue(row.get("gene_name"))
        );
    }

    private List<Map<String, Object>> collapseGeneralRows(
            List<Map<String, Object>> rows,
            String referenceMode
    ) {
        boolean markerReference = "p2g_markers".equals(referenceMode);
        Map<String, Map<String, Object>> rowsByLink = new LinkedHashMap<>();
        Map<String, Set<String>> markerTypesByLink = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String linkKey = stringValue(row.get("peak_name")) + '|'
                    + stringValue(row.get("gene_name")) + '|'
                    + stringValue(row.get("dataset_id"));
            rowsByLink.putIfAbsent(linkKey, row);
            if (markerReference) {
                markerTypesByLink.computeIfAbsent(linkKey, ignored -> new LinkedHashSet<>())
                        .add(normalizeSignalType(stringValue(row.get("signal_type"))));
            }
        }

        for (Map.Entry<String, Map<String, Object>> entry : rowsByLink.entrySet()) {
            Map<String, Object> row = entry.getValue();
            row.remove("cell_type");
            row.remove("context_label");
            row.remove("cluster_label");
            if (markerReference) {
                row.remove("signal_type");
                row.put("has_marker_gene", true);
                row.put("gene_marker_types", orderedSignalTypes(markerTypesByLink.get(entry.getKey())));
            }
        }
        return new ArrayList<>(rowsByLink.values());
    }

    private void annotateRawRows(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }

        List<String> datasetIds = rows.stream()
                .map(row -> stringValue(row.get("dataset_id")))
                .distinct()
                .toList();
        List<String> genes = rows.stream()
                .map(row -> stringValue(row.get("gene_name")))
                .distinct()
                .toList();
        List<String> peakNames = rows.stream()
                .map(row -> stringValue(row.get("peak_name")))
                .distinct()
                .toList();

        Map<DatasetFeatureKey, Set<String>> markerTypes = new HashMap<>();
        for (Map<String, Object> marker : mapper.selectGeneMarkerSignals(datasetIds, genes)) {
            DatasetFeatureKey key = new DatasetFeatureKey(
                    stringValue(marker.get("dataset_id")),
                    stringValue(marker.get("gene_symbol"))
            );
            String signalType = normalizeSignalType(stringValue(marker.get("signal_type")));
            markerTypes.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(signalType);
        }

        Set<DatasetFeatureKey> markerPeaks = new LinkedHashSet<>();
        for (Map<String, Object> marker : mapper.selectMarkerPeakKeys(datasetIds, peakNames)) {
            markerPeaks.add(new DatasetFeatureKey(
                    stringValue(marker.get("dataset_id")),
                    stringValue(marker.get("peak_name"))
            ));
        }

        for (Map<String, Object> row : rows) {
            String datasetId = stringValue(row.get("dataset_id"));
            DatasetFeatureKey geneKey = new DatasetFeatureKey(datasetId, stringValue(row.get("gene_name")));
            DatasetFeatureKey peakKey = new DatasetFeatureKey(datasetId, stringValue(row.get("peak_name")));
            List<String> types = orderedSignalTypes(markerTypes.get(geneKey));
            row.put("gene_marker_types", types);
            row.put("has_marker_gene", !types.isEmpty());
            row.put("has_marker_peak", markerPeaks.contains(peakKey));
        }
    }

    private void annotateMarkerRows(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            String signalType = normalizeSignalType(stringValue(row.get("signal_type")));
            row.put("signal_type", signalType);
            row.put("gene_marker_types", List.of(signalType));
            row.put("has_marker_gene", true);
        }
    }

    private List<PeakGeneContextResponse.CellTypeResult> buildCellTypeResults(List<Map<String, Object>> rows) {
        Map<String, CellTypeAccumulator> accumulators = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String cellType = stringValue(row.get("cell_type"));
            String gene = stringValue(row.get("gene_name"));
            CellTypeAccumulator accumulator = accumulators.computeIfAbsent(cellType, CellTypeAccumulator::new);
            accumulator.add(
                    stringValue(row.get("peak_name")),
                    gene,
                    stringValue(row.get("dataset_id"))
            );
        }

        List<PeakGeneContextResponse.CellTypeResult> results = new ArrayList<>(accumulators.size());
        for (CellTypeAccumulator accumulator : accumulators.values()) {
            PeakGeneContextResponse.CellTypeResult result = new PeakGeneContextResponse.CellTypeResult();
            result.setCellType(accumulator.cellType);
            result.setEvidenceCount(accumulator.evidenceCount);
            result.setPeakCount(accumulator.peaks.size());
            result.setGeneCount(accumulator.genes.size());
            result.setDatasetCount(accumulator.datasets.size());
            result.setGeneDetails(accumulator.geneDetails());
            results.add(result);
        }
        return results;
    }

    private PeakGeneContextResponse buildResponse(
            ValidatedRequest request,
            List<Map<String, Object>> matchedRows,
            List<Map<String, Object>> returnedRows,
            List<PeakGeneContextResponse.CellTypeResult> cellTypeResults
    ) {
        PeakGeneContextResponse.Summary summary = new PeakGeneContextResponse.Summary();
        summary.setTotalPairs(matchedRows.size());
        summary.setUniquePeaks(uniqueCount(matchedRows, "peak_name"));
        summary.setUniqueGenes(uniqueCount(matchedRows, "gene_name"));
        summary.setUniqueDatasets(uniqueCount(matchedRows, "dataset_id"));
        summary.setUniqueCellTypes("cell_type".equals(request.resultType())
                ? uniqueCount(matchedRows, "cell_type")
                : 0);
        if (!cellTypeResults.isEmpty()) {
            PeakGeneContextResponse.CellTypeResult top = cellTypeResults.get(0);
            summary.setTopCellType(top.getCellType());
            summary.setTopCellTypeEvidence(top.getEvidenceCount());
        }

        List<PeakGeneContextResponse.PairDetail> pairs = returnedRows.stream()
                .map(this::pairDetail)
                .toList();
        PeakGeneContextResponse response = new PeakGeneContextResponse();
        response.setSummary(summary);
        response.setCellTypeResults(cellTypeResults);
        response.setPairs(pairs);
        boolean includeCellType = "cell_type".equals(request.resultType());
        response.setNetworkData(buildNetwork(selectNetworkRows(returnedRows, includeCellType), includeCellType));
        return response;
    }

    private int uniqueCount(List<Map<String, Object>> rows, String field) {
        return (int) rows.stream().map(row -> stringValue(row.get(field))).distinct().count();
    }

    @SuppressWarnings("unchecked")
    private PeakGeneContextResponse.PairDetail pairDetail(Map<String, Object> row) {
        PeakGeneContextResponse.PairDetail detail = new PeakGeneContextResponse.PairDetail();
        detail.setPeakName(stringValue(row.get("peak_name")));
        detail.setChromosome(stringValue(row.get("chromosome")));
        detail.setPeakStart(longValue(row.get("peak_start")));
        detail.setPeakEnd(longValue(row.get("peak_end")));
        detail.setGeneName(stringValue(row.get("gene_name")));
        detail.setCellType(nullableString(row.get("cell_type")));
        detail.setContextLabel(nullableString(row.get("context_label")));
        detail.setClusterLabel(nullableString(row.get("cluster_label")));
        detail.setDatasetId(stringValue(row.get("dataset_id")));
        detail.setLinkScore(nullableDouble(row.get("link_score")));
        detail.setLinkFdr(nullableDouble(row.get("link_fdr")));
        detail.setHasMarkerPeak(booleanValue(row.get("has_marker_peak")));
        detail.setHasMarkerGene(booleanValue(row.get("has_marker_gene")));
        detail.setSignalType(nullableString(row.get("signal_type")));
        detail.setGeneMarkerTypes((List<String>) row.get("gene_marker_types"));
        return detail;
    }

    private List<Map<String, Object>> selectNetworkRows(
            List<Map<String, Object>> rows,
            boolean includeCellType
    ) {
        Map<GenePeakKey, Integer> matchCounts = new LinkedHashMap<>();
        Map<GenePeakKey, Double> maxLinkScores = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            GenePeakKey key = new GenePeakKey(
                    stringValue(row.get("gene_name")),
                    stringValue(row.get("peak_name"))
            );
            matchCounts.merge(key, 1, Integer::sum);
            if (!includeCellType) {
                maxLinkScores.merge(key, doubleValue(row.get("link_score")), Math::max);
            }
        }

        Map<String, List<GenePeakKey>> linksByGene = new LinkedHashMap<>();
        for (GenePeakKey key : matchCounts.keySet()) {
            linksByGene.computeIfAbsent(key.gene(), ignored -> new ArrayList<>()).add(key);
        }

        Set<GenePeakKey> selectedLinks = new LinkedHashSet<>();
        for (List<GenePeakKey> geneLinks : linksByGene.values()) {
            Comparator<GenePeakKey> comparator = includeCellType
                    ? Comparator.comparingInt((GenePeakKey key) -> matchCounts.get(key)).reversed()
                    : Comparator.comparingDouble((GenePeakKey key) -> maxLinkScores.get(key)).reversed();
            geneLinks.sort(comparator.thenComparing(GenePeakKey::peak));
            geneLinks.stream().limit(NETWORK_PEAK_LIMIT_PER_GENE).forEach(selectedLinks::add);
        }

        return rows.stream()
                .filter(row -> selectedLinks.contains(new GenePeakKey(
                        stringValue(row.get("gene_name")),
                        stringValue(row.get("peak_name"))
                )))
                .toList();
    }

    private PeakGeneContextResponse.NetworkData buildNetwork(List<Map<String, Object>> rows, boolean includeCellType) {
        Map<String, String> nodeNames = new LinkedHashMap<>();
        Map<String, String> nodeCategories = new LinkedHashMap<>();
        Map<String, Integer> nodeWeights = new LinkedHashMap<>();
        Map<String, EdgeAccumulator> edgeAccumulators = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String peak = stringValue(row.get("peak_name"));
            String gene = stringValue(row.get("gene_name"));
            Double score = includeCellType ? null : doubleValue(row.get("link_score"));
            String peakId = "p_" + peak;
            String geneId = "g_" + gene;
            addNode(nodeNames, nodeCategories, nodeWeights, peakId, peak, "peak");
            addNode(nodeNames, nodeCategories, nodeWeights, geneId, gene, "gene");
            addEdge(edgeAccumulators, peakId, geneId, score);

            if (includeCellType) {
                String cellType = stringValue(row.get("cell_type"));
                String cellTypeId = "c_" + cellType;
                addNode(nodeNames, nodeCategories, nodeWeights, cellTypeId, cellType, "cellType");
                addEdge(edgeAccumulators, geneId, cellTypeId, score);
            }
        }

        List<PeakGeneContextResponse.NetworkNode> nodes = nodeNames.entrySet().stream()
                .map(entry -> node(
                        entry.getKey(),
                        entry.getValue(),
                        nodeCategories.get(entry.getKey()),
                        nodeWeights.get(entry.getKey())
                ))
                .toList();
        List<PeakGeneContextResponse.NetworkEdge> edges = edgeAccumulators.values().stream()
                .map(this::edge)
                .toList();

        PeakGeneContextResponse.NetworkData network = new PeakGeneContextResponse.NetworkData();
        network.setNodes(nodes);
        network.setEdges(edges);
        network.setPeakLimitPerGene(NETWORK_PEAK_LIMIT_PER_GENE);
        return network;
    }

    private void addNode(
            Map<String, String> nodeNames,
            Map<String, String> nodeCategories,
            Map<String, Integer> nodeWeights,
            String id,
            String name,
            String category
    ) {
        nodeNames.putIfAbsent(id, name);
        nodeCategories.putIfAbsent(id, category);
        nodeWeights.merge(id, 1, Integer::sum);
    }

    private void addEdge(Map<String, EdgeAccumulator> edges, String source, String target, Double score) {
        String key = source + '\u0000' + target;
        edges.computeIfAbsent(key, ignored -> new EdgeAccumulator(source, target)).add(score);
    }

    private void sortCellTypeResults(List<PeakGeneContextResponse.CellTypeResult> results) {
        results.sort(Comparator.comparingInt(PeakGeneContextResponse.CellTypeResult::getEvidenceCount)
                .reversed()
                .thenComparing(PeakGeneContextResponse.CellTypeResult::getCellType));
    }

    private PeakGeneContextResponse.NetworkNode node(String id, String name, String category, int value) {
        PeakGeneContextResponse.NetworkNode node = new PeakGeneContextResponse.NetworkNode();
        node.setId(id);
        node.setName(name);
        node.setCategory(category);
        node.setValue(value);
        return node;
    }

    private PeakGeneContextResponse.NetworkEdge edge(EdgeAccumulator accumulator) {
        PeakGeneContextResponse.NetworkEdge edge = new PeakGeneContextResponse.NetworkEdge();
        edge.setSource(accumulator.source);
        edge.setTarget(accumulator.target);
        edge.setEvidenceCount(accumulator.count);
        edge.setWeight(accumulator.scoreCount == 0 ? null : accumulator.scoreTotal / accumulator.scoreCount);
        return edge;
    }

    private static List<String> orderedSignalTypes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().sorted(Comparator
                        .comparingInt(PeakGeneContextService::signalTypeRank)
                        .thenComparing(value -> value))
                .toList();
    }

    private static int signalTypeRank(String value) {
        if ("gene_expression".equals(value)) {
            return 0;
        }
        if ("gene_score".equals(value)) {
            return 1;
        }
        return 2;
    }

    private static String normalizeSignalType(String value) {
        return "gene_exp".equalsIgnoreCase(value) ? "gene_expression" : value.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        String normalized = nullableText(value);
        if (normalized == null) {
            throw badRequest(field + " is required.");
        }
        return normalized;
    }

    private static String nullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            throw new IllegalStateException("Required query column is null.");
        }
        return value.toString();
    }

    private static String nullableString(Object value) {
        return value == null ? null : value.toString();
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Expected numeric query column.");
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Expected numeric query column.");
    }

    private static Double nullableDouble(Object value) {
        return value == null ? null : doubleValue(value);
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        throw new IllegalStateException("Expected boolean query column.");
    }

    private record ValidatedRequest(
            List<Map<String, Object>> peaks,
            List<String> genes,
            String tissue,
            String datasetId,
            String referenceMode,
            String resultType,
            int minOverlapBp,
            Integer maxReturnedLinks
    ) { }

    private record DatasetFeatureKey(String datasetId, String feature) { }

    private record GenePeakKey(String gene, String peak) { }

    private record CellTypeLinkKey(String cellType, String peak, String gene) { }

    private static final class CellTypeAccumulator {
        private final String cellType;
        private final Set<String> peaks = new LinkedHashSet<>();
        private final Set<String> genes = new LinkedHashSet<>();
        private final Set<String> datasets = new LinkedHashSet<>();
        private final Map<String, GeneAccumulator> geneAccumulators = new LinkedHashMap<>();
        private int evidenceCount;

        private CellTypeAccumulator(String cellType) {
            this.cellType = cellType;
        }

        private void add(String peak, String gene, String dataset) {
            evidenceCount++;
            peaks.add(peak);
            genes.add(gene);
            datasets.add(dataset);
            geneAccumulators.computeIfAbsent(gene, GeneAccumulator::new).add();
        }

        private List<PeakGeneContextResponse.GeneDetail> geneDetails() {
            return geneAccumulators.values().stream()
                    .map(GeneAccumulator::toDetail)
                    .sorted(Comparator.comparingInt(PeakGeneContextResponse.GeneDetail::getCount).reversed()
                            .thenComparing(PeakGeneContextResponse.GeneDetail::getGene))
                    .toList();
        }
    }

    private static final class GeneAccumulator {
        private final String gene;
        private int count;

        private GeneAccumulator(String gene) {
            this.gene = gene;
        }

        private void add() {
            count++;
        }

        private PeakGeneContextResponse.GeneDetail toDetail() {
            PeakGeneContextResponse.GeneDetail detail = new PeakGeneContextResponse.GeneDetail();
            detail.setGene(gene);
            detail.setCount(count);
            return detail;
        }
    }

    private static final class EdgeAccumulator {
        private final String source;
        private final String target;
        private int count;
        private int scoreCount;
        private double scoreTotal;

        private EdgeAccumulator(String source, String target) {
            this.source = source;
            this.target = target;
        }

        private void add(Double score) {
            count++;
            if (score != null) {
                scoreCount++;
                scoreTotal += score;
            }
        }
    }
}
