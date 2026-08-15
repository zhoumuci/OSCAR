package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsIntersectSummary;
import com.oscar.backend.entity.BedtoolsOverlapRecord;
import com.oscar.backend.entity.FeatureOccurrenceResponse;
import com.oscar.backend.entity.FeatureOccurrenceResponse.CellContextRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.DatasetRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceDatasetEntry;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceTopCellType;
import com.oscar.backend.mapper.FeatureOccurrenceMapper;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.FeatureOccurrenceAggregation;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.GeneEnhancerRegion;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.GeneOccurrenceContextRow;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.GeneRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
@Service
public class FeatureOccurrenceService {

    private static final Logger log = LoggerFactory.getLogger(FeatureOccurrenceService.class);
    private static final String DEFAULT_DOMAIN = "integration";
    private static final String DEFAULT_GENOME_BUILD = "hg38";
    private static final int DEFAULT_RANKING_LIMIT = 10;

    private final FeatureOccurrenceMapper mapper;
    private final BedtoolsQueryService bedtoolsQueryService;

    public FeatureOccurrenceService(FeatureOccurrenceMapper mapper, BedtoolsQueryService bedtoolsQueryService) {
        this.mapper = mapper;
        this.bedtoolsQueryService = bedtoolsQueryService;
    }

    @org.springframework.cache.annotation.Cacheable(value = "featureOccurrence",
            key = "#type + ':' + (#gene != null ? #gene : '') + ':' + (#chrom != null ? #chrom : '') + ':' + #start + '-' + #end + ':' + (#strand != null ? #strand : '') + ':' + #domain + ':' + #contextOnly + ':' + #full",
            sync = true)
    public FeatureOccurrenceResponse getOccurrence(
            String type, String gene, String chrom, Long start, Long end, String strand, String domain, boolean contextOnly, boolean full) {
        String nt = normalizeType(type);
        String nd = normalizeDomain(domain);
        if ("gene".equals(nt)) {
            if (isBlank(gene)) throw new FeatureOccurrenceException("INVALID_PARAMETER", "gene is required when type=gene", HttpStatus.BAD_REQUEST);
            return buildGeneResponse(
                    gene.trim().toUpperCase(Locale.ROOT),
                    nd,
                    providedGeneRegion(chrom, start, end, strand),
                    full
            );
        }
        if (isBlank(chrom) || start == null || end == null)
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "chrom, start, end are required when type=peak", HttpStatus.BAD_REQUEST);
        if (start < 0 || end <= start)
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "start must be >=0 and end > start", HttpStatus.BAD_REQUEST);
        return buildPeakResponse(chrom.trim(), start, end, nd, full);
    }

    private FeatureOccurrenceResponse buildGeneResponse(String gene, String domain, GeneRegion providedRegion, boolean full) {
        long overallStarted = System.nanoTime();

        long contextsQueryStarted = System.nanoTime();
        List<GeneOccurrenceContextRow> contextRows = safe(mapper.selectGeneOccurrenceContexts(gene, domain));
        long contextsQueryMillis = elapsedMillis(contextsQueryStarted);

        long summarizeStarted = System.nanoTime();
        GeneOccurrenceSummary summary = summarizeGeneContexts(contextRows, full);
        long summarizeMillis = elapsedMillis(summarizeStarted);

        long responseBuildStarted = System.nanoTime();
        FeatureOccurrenceResponse r = new FeatureOccurrenceResponse();
        r.setFeatureType("gene"); r.setFeatureId(gene); r.setDomain(domain); r.setAvailable(true);
        r.setTotalOccurrences(summary.totalOccurrences());
        r.setDatasetCount(summary.datasetCount());
        r.setCellTypeCount(summary.cellTypeCount());
        r.setClusterCount(summary.clusterCount());
        r.setTopCellTypes(summary.topCellTypes());
        r.setDatasets(summary.datasets());
        r.setDatasetRanking(summary.datasetRanking());
        r.setCellContextRanking(summary.cellContextRanking());
        r.setGenomeBuild(DEFAULT_GENOME_BUILD);
        long responseBuildMillis = elapsedMillis(responseBuildStarted);

        boolean usedProvidedRegion = isValidRegion(providedRegion);
        long coordinateResolutionStarted = System.nanoTime();
        GeneRegion geneRegion = usedProvidedRegion
                ? providedRegion
                : resolveGeneRegion(gene, domain, "occurrence");
        long coordinateResolutionMillis = elapsedMillis(coordinateResolutionStarted);
        if (isValidRegion(geneRegion)) {
            r.setGeneBodyRegion(regionString(geneRegion.getChromosome(), geneRegion.getStart(), geneRegion.getEnd()));
            r.setPromoterRegion(promoterRegion(geneRegion));
        }

        log.info(
                "Gene occurrence probe gene={} domain={} contextRows={} totalOccurrences={} "
                        + "contextsQueryMillis={} summarizeMillis={} responseBuildMillis={} "
                        + "coordinateResolutionMillis={} providedRegion={} overallMillis={}",
                gene,
                domain,
                contextRows.size(),
                summary.totalOccurrences(),
                contextsQueryMillis,
                summarizeMillis,
                responseBuildMillis,
                coordinateResolutionMillis,
                usedProvidedRegion,
                elapsedMillis(overallStarted)
        );
        return r;
    }

    public List<Map<String, Object>> getEnhancerRegions(String gene, String enhancerType) {
        String normalizedGene = normalizeGene(gene);
        String normalizedEnhancerType = normalizeEnhancerType(enhancerType);
        return mapper.selectGeneEnhancerRegionMaps(normalizedGene, normalizedEnhancerType);
    }

    @org.springframework.cache.annotation.Cacheable(
            value = "geneExpression",
            key = "#gene + ':' + #platform + ':' + #full",
            sync = true
    )
    public List<Map<String, Object>> getExpression(String gene, String platform, boolean full) {
        String normalizedGene = normalizeGene(gene);
        String normalizedPlatform = normalizeExpressionPlatform(platform);
        long queryStarted = System.nanoTime();
        List<Map<String, Object>> rows = full
                ? mapper.selectAllGeneExpression(normalizedGene, normalizedPlatform)
                : mapper.selectGeneExpressionTop30(normalizedGene, normalizedPlatform);
        long queryMillis = elapsedMillis(queryStarted);
        log.info(
                "Gene expression probe gene={} platform={} full={} rows={} queryMillis={} overallMillis={}",
                normalizedGene,
                normalizedPlatform,
                full,
                rows != null ? rows.size() : 0,
                queryMillis,
                queryMillis
        );
        return rows;
    }

    public BedtoolsIntersectResponse getGeneRegulatoryAnnotation(
            String gene,
            String chrom,
            Long start,
            Long end,
            String strand,
            String mode,
            String annotationType,
            String domain,
            String genomeBuild
    ) {
        long overallStarted = System.nanoTime();
        String normalizedGene = normalizeGene(gene);
        String normalizedMode = normalizeRegulatoryMode(mode);
        String normalizedDomain = normalizeDomain(domain);
        String normalizedGenomeBuild = normalizeGenomeBuild(genomeBuild);
        BedtoolsAnnotationType resolvedAnnotationType = resolveAnnotationType(normalizedMode, annotationType);
        long targetPageStarted = System.nanoTime();
        QueryTargetPage targetPage = queryTargetPage(
                normalizedGene,
                normalizedMode,
                normalizedDomain,
                providedGeneRegion(chrom, start, end, strand)
        );
        long targetPageMillis = elapsedMillis(targetPageStarted);

        List<BedtoolsOverlapRecord> allRecords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long bedtoolsMillis = 0L;
        for (QueryTarget target : targetPage.targets()) {
            BedtoolsIntersectRequest request = new BedtoolsIntersectRequest();
            request.setRegion(target.bedRegion());
            request.setAnnotationTypes(List.of(resolvedAnnotationType.value()));
            request.setMinOverlapBp(1);
            long bedtoolsStarted = System.nanoTime();
            BedtoolsIntersectResponse response = bedtoolsQueryService.referenceIntersectAll(normalizedGenomeBuild, request);
            bedtoolsMillis += elapsedMillis(bedtoolsStarted);
            warnings.addAll(safe(response.getWarnings()));
            for (BedtoolsOverlapRecord record : safe(response.getRecords())) {
                record.setQueryRegion(target.displayRegion());
                if (target.enhancerRegion() != null) record.setQueryEnhancerRegion(target.enhancerRegion());
                if (target.cellType() != null) record.setCellType(target.cellType());
                allRecords.add(record);
            }
        }
        long postProcessStarted = System.nanoTime();
        allRecords.sort(Comparator
                .comparing((BedtoolsOverlapRecord r) -> nullToEmpty(r.getQueryRegion()))
                .thenComparing(r -> nullToEmpty(r.getFeatureRegion()))
                .thenComparing(r -> nullToEmpty(r.getFeatureId())));

        long total = allRecords.size();

        BedtoolsIntersectSummary summary = new BedtoolsIntersectSummary();
        summary.setTotalHits(total);
        summary.setByAnnotationType(Map.of(resolvedAnnotationType.value(), total));

        BedtoolsIntersectResponse response = new BedtoolsIntersectResponse();
        response.setStatus("SUCCESS");
        response.setMessage("OK");
        response.setDatasetId(null);
        response.setDomain("reference");
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setCoordinateSystem("BED 0-based half-open");
        response.setSelectedAnnotationTypes(List.of(resolvedAnnotationType.value()));
        response.setPage(1);
        response.setPageSize(allRecords.size());
        response.setTotal(total);
        response.setSummary(summary);
        response.setRecords(allRecords);
        response.setWarnings(warnings);
        log.info(
                "Gene regulatory probe gene={} domain={} mode={} annotationType={} targets={} records={} total={} "
                        + "targetPageMillis={} bedtoolsMillis={} postProcessMillis={} overallMillis={}",
                normalizedGene,
                normalizedDomain,
                normalizedMode,
                resolvedAnnotationType.value(),
                targetPage.targets().size(),
                allRecords.size(),
                total,
                targetPageMillis,
                bedtoolsMillis,
                elapsedMillis(postProcessStarted),
                elapsedMillis(overallStarted)
        );
        return response;
    }

    private FeatureOccurrenceResponse buildPeakResponse(String chrom, long start, long end, String domain, boolean full) {
        FeatureOccurrenceAggregation agg = mapper.selectPeakAggregation(chrom, start, end, domain);
        int rankingLimit = full ? Integer.MAX_VALUE : DEFAULT_RANKING_LIMIT;
        int datasetEntryLimit = full ? Integer.MAX_VALUE : 100;
        List<FeatureOccurrenceTopCellType> top = mapper.selectPeakTopCellTypes(
                chrom, start, end, domain, rankingLimit
        );
        List<FeatureOccurrenceDatasetEntry> datasets = mapper.selectPeakDatasetEntries(
                chrom, start, end, domain, datasetEntryLimit
        );
        List<DatasetRankingItem> dr = mapper.selectPeakDatasetRanking(chrom, start, end, domain, rankingLimit);
        List<CellContextRankingItem> cr = mapper.selectPeakCellContextRanking(chrom, start, end, domain, rankingLimit);

        String fid = chrom + ":" + start + "-" + end;
        FeatureOccurrenceResponse r = new FeatureOccurrenceResponse();
        r.setFeatureType("peak"); r.setFeatureId(fid); r.setDomain(domain); r.setAvailable(true);
        r.setTotalOccurrences(agg != null ? (int) agg.getTotal() : 0);
        r.setDatasetCount(agg != null ? (int) agg.getDatasetCount() : 0);
        r.setCellTypeCount(agg != null ? (int) agg.getCellTypeCount() : 0);
        r.setClusterCount(agg != null ? (int) agg.getClusterCount() : 0);
        r.setTopCellTypes(safe(top)); r.setDatasets(safe(datasets));
        r.setDatasetRanking(safe(dr)); r.setCellContextRanking(safe(cr));
        return r;
    }

    private <T> List<T> safe(List<T> list) { return list != null ? list : List.of(); }

    private GeneOccurrenceSummary summarizeGeneContexts(List<GeneOccurrenceContextRow> rows, boolean full) {
        long totalOccurrences = 0L;
        Set<String> datasetIds = new HashSet<>();
        Set<String> cellTypes = new HashSet<>();
        Set<String> clusters = new HashSet<>();
        Map<String, Long> topCellTypeCounts = new LinkedHashMap<>();
        Map<String, DatasetStats> datasetStats = new LinkedHashMap<>();
        Map<String, CellContextStats> cellContextStats = new LinkedHashMap<>();
        List<FeatureOccurrenceDatasetEntry> datasets = new ArrayList<>();

        for (GeneOccurrenceContextRow row : rows) {
            String datasetId = trimToNull(row.getDatasetId());
            String cellType = trimToNull(row.getCellType());
            String cluster = trimToNull(row.getCluster());
            String clusterContextKey = datasetId != null && cluster != null
                    ? datasetId + '\u001F' + cluster
                    : null;
            String cellTypeLabel = cellType != null ? cellType : "Unknown";
            long recordCount = Math.max(0L, row.getRecordCount());

            totalOccurrences += recordCount;
            if (datasetId != null) datasetIds.add(datasetId);
            if (cellType != null) cellTypes.add(cellType);
            if (clusterContextKey != null) clusters.add(clusterContextKey);
            topCellTypeCounts.merge(cellTypeLabel, recordCount, Long::sum);

            if (full || datasets.size() < 100) {
                FeatureOccurrenceDatasetEntry entry = new FeatureOccurrenceDatasetEntry();
                entry.setDatasetId(datasetId);
                entry.setCellType(cellType);
                entry.setCluster(cluster);
                entry.setOccurrenceCount(safeCount(recordCount));
                datasets.add(entry);
            }

            if (datasetId != null) {
                DatasetStats stats = datasetStats.computeIfAbsent(datasetId, ignored -> new DatasetStats());
                if (stats.sampleName == null && row.getSampleName() != null) stats.sampleName = row.getSampleName();
                stats.recordCount += recordCount;
                if (cellType != null) stats.cellTypes.add(cellType);
                if (cluster != null) stats.clusters.add(cluster);
            }

            CellContextStats contextStats = cellContextStats.computeIfAbsent(
                    cellTypeLabel,
                    ignored -> new CellContextStats()
            );
            contextStats.recordCount += recordCount;
            if (datasetId != null) contextStats.datasetIds.add(datasetId);
            if (clusterContextKey != null) contextStats.clusters.add(clusterContextKey);
        }

        List<FeatureOccurrenceTopCellType> topCellTypes = new ArrayList<>();
        for (Map.Entry<String, Long> entry : topCellTypeCounts.entrySet()) {
            FeatureOccurrenceTopCellType item = new FeatureOccurrenceTopCellType();
            item.setCellType(entry.getKey());
            item.setCount(safeCount(entry.getValue()));
            topCellTypes.add(item);
        }
        topCellTypes.sort(Comparator
                .comparingInt(FeatureOccurrenceTopCellType::getCount)
                .reversed()
                .thenComparing(item -> nullToEmpty(item.getCellType())));

        List<DatasetRankingItem> datasetRanking = new ArrayList<>();
        for (Map.Entry<String, DatasetStats> entry : datasetStats.entrySet()) {
            DatasetRankingItem item = new DatasetRankingItem();
            item.setDatasetId(entry.getKey());
            item.setSampleName(entry.getValue().sampleName);
            item.setRecordCount(safeCount(entry.getValue().recordCount));
            item.setCellContextCount(entry.getValue().cellTypes.size());
            item.setClusterCount(entry.getValue().clusters.size());
            datasetRanking.add(item);
        }
        datasetRanking.sort(Comparator
                .comparingInt(DatasetRankingItem::getRecordCount)
                .reversed()
                .thenComparing(item -> nullToEmpty(item.getDatasetId())));

        List<CellContextRankingItem> cellContextRanking = new ArrayList<>();
        for (Map.Entry<String, CellContextStats> entry : cellContextStats.entrySet()) {
            CellContextRankingItem item = new CellContextRankingItem();
            item.setCellType(entry.getKey());
            item.setRecordCount(safeCount(entry.getValue().recordCount));
            item.setDatasetCount(entry.getValue().datasetIds.size());
            item.setClusterCount(entry.getValue().clusters.size());
            cellContextRanking.add(item);
        }
        cellContextRanking.sort(Comparator
                .comparingInt(CellContextRankingItem::getRecordCount)
                .reversed()
                .thenComparing(item -> nullToEmpty(item.getCellType())));

        int rankingLimit = full ? Integer.MAX_VALUE : DEFAULT_RANKING_LIMIT;
        return new GeneOccurrenceSummary(
                safeCount(totalOccurrences),
                datasetIds.size(),
                cellTypes.size(),
                clusters.size(),
                limit(topCellTypes, rankingLimit),
                datasets,
                limit(datasetRanking, rankingLimit),
                limit(cellContextRanking, rankingLimit)
        );
    }

    private <T> List<T> limit(List<T> values, int maximumSize) {
        if (values.size() <= maximumSize) return values;
        return new ArrayList<>(values.subList(0, maximumSize));
    }

    private int safeCount(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private QueryTargetPage queryTargetPage(
            String gene,
            String mode,
            String domain,
            GeneRegion providedRegion
    ) {
        if ("super_enhancer".equals(mode) || "typical_enhancer".equals(mode)) {
            String enhancerType = "super_enhancer".equals(mode) ? "SE" : "TE";
            List<GeneEnhancerRegion> enhancerRegions = mapper.selectAllGeneEnhancerRegions(gene, enhancerType);
            List<QueryTarget> targets = new ArrayList<>();
            for (GeneEnhancerRegion region : enhancerRegions) {
                if (!isValidEnhancerRegion(region)) {
                    continue;
                }
                String displayRegion = regionString(region.getChromosome(), region.getRegionStart(), region.getRegionEnd());
                String bedRegion = regionString(
                        region.getChromosome(),
                        Math.max(0L, region.getRegionStart() - 1L),
                        region.getRegionEnd()
                );
                targets.add(new QueryTarget(displayRegion, bedRegion, displayRegion, trimToNull(region.getBiosampleName())));
            }
            return new QueryTargetPage(targets);
        }

        GeneRegion geneRegion = isValidRegion(providedRegion)
                ? providedRegion
                : resolveGeneRegion(gene, domain, "regulatory:" + mode);
        if (!isValidRegion(geneRegion)) {
            return new QueryTargetPage(List.of());
        }
        if ("promoter".equals(mode)) {
            String region = promoterRegion(geneRegion);
            return new QueryTargetPage(region == null ? List.of() : List.of(new QueryTarget(region, region, null, null)));
        }
        String region = regionString(geneRegion.getChromosome(), geneRegion.getStart(), geneRegion.getEnd());
        return new QueryTargetPage(List.of(new QueryTarget(region, region, null, null)));
    }

    private BedtoolsAnnotationType resolveAnnotationType(String mode, String annotationTypeOverride) {
        if (annotationTypeOverride != null && !annotationTypeOverride.isBlank()) {
            BedtoolsAnnotationType resolved = BedtoolsAnnotationType.fromValue(annotationTypeOverride.trim().toLowerCase(Locale.ROOT));
            if (resolved == null) {
                throw new FeatureOccurrenceException(
                        "INVALID_PARAMETER",
                        "Unknown annotationType: " + annotationTypeOverride,
                        HttpStatus.BAD_REQUEST
                );
            }
            return resolved;
        }
        return switch (mode) {
            case "promoter" -> BedtoolsAnnotationType.TSS_PROMOTER;
            case "super_enhancer" -> BedtoolsAnnotationType.SUPER_ENHANCER;
            case "typical_enhancer" -> BedtoolsAnnotationType.ENHANCER;
            default -> BedtoolsAnnotationType.GENE;
        };
    }

    private GeneRegion providedGeneRegion(String chrom, Long start, Long end, String strand) {
        GeneRegion region = new GeneRegion();
        region.setChromosome(trimToNull(chrom));
        region.setStart(start);
        region.setEnd(end);
        region.setStrand(trimToNull(strand));
        normalizeGeneRegion(region);
        return isValidRegion(region) ? region : null;
    }

    private GeneRegion resolveGeneRegion(String gene, String domain, String phase) {
        long overallStarted = System.nanoTime();
        long markerQueryStarted = System.nanoTime();
        GeneRegion region = normalizeGeneRegion(mapper.selectGeneRegionFromMarkerGene(gene, domain));
        long markerQueryMillis = elapsedMillis(markerQueryStarted);
        if (isValidRegion(region)) {
            log.info(
                    "Gene coordinate probe phase={} gene={} domain={} source=marker_gene found=true "
                            + "markerQueryMillis={} linkedQueryMillis=0 overallMillis={}",
                    phase,
                    gene,
                    domain,
                    markerQueryMillis,
                    elapsedMillis(overallStarted)
            );
            return region;
        }
        long linkedQueryStarted = System.nanoTime();
        GeneRegion linkedRegion = normalizeGeneRegion(mapper.selectGeneRegionFromLinkedRegion(gene, domain));
        long linkedQueryMillis = elapsedMillis(linkedQueryStarted);
        log.info(
                "Gene coordinate probe phase={} gene={} domain={} source=marker_linked_region found={} "
                        + "markerQueryMillis={} linkedQueryMillis={} overallMillis={}",
                phase,
                gene,
                domain,
                isValidRegion(linkedRegion),
                markerQueryMillis,
                linkedQueryMillis,
                elapsedMillis(overallStarted)
        );
        return linkedRegion;
    }

    private String normalizeType(String type) {
        String n = trimToNull(type);
        if (n == null) throw new FeatureOccurrenceException("INVALID_PARAMETER", "type is required (gene or peak)", HttpStatus.BAD_REQUEST);
        String l = n.toLowerCase(Locale.ROOT);
        if (!"gene".equals(l) && !"peak".equals(l))
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "type must be gene or peak, got: " + n, HttpStatus.BAD_REQUEST);
        return l;
    }

    private String normalizeDomain(String value) {
        String n = trimToNull(value);
        if (n == null) return DEFAULT_DOMAIN;
        String l = n.toLowerCase(Locale.ROOT);
        if (!List.of("integration", "rna", "atac").contains(l))
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "domain must be integration, rna, or atac", HttpStatus.BAD_REQUEST);
        return l;
    }

    private String normalizeGenomeBuild(String value) {
        String n = trimToNull(value);
        return n == null ? DEFAULT_GENOME_BUILD : n.toLowerCase(Locale.ROOT);
    }

    private String normalizeGene(String gene) {
        if (isBlank(gene)) {
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "gene is required", HttpStatus.BAD_REQUEST);
        }
        return gene.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExpressionPlatform(String platform) {
        String normalized = trimToNull(platform);
        if (normalized == null) {
            throw new FeatureOccurrenceException(
                    "INVALID_PARAMETER",
                    "platform is required",
                    HttpStatus.BAD_REQUEST
            );
        }
        String value = normalized.toUpperCase(Locale.ROOT);
        if (!List.of("GTEX", "CCLE", "ENCODE", "TCGA").contains(value)) {
            throw new FeatureOccurrenceException(
                    "INVALID_PARAMETER",
                    "platform must be GTEX, CCLE, ENCODE, or TCGA",
                    HttpStatus.BAD_REQUEST
            );
        }
        return value;
    }

    private String normalizeEnhancerType(String value) {
        String n = trimToNull(value);
        if (n == null) {
            return "SE";
        }
        String key = n.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "SUPER_ENHANCER", "SUPERENHANCER", "SE" -> "SE";
            case "TYPICAL_ENHANCER", "TYPICALENHANCER", "ENHANCER", "TE" -> "TE";
            default -> throw new FeatureOccurrenceException(
                    "INVALID_PARAMETER",
                    "enhancerType must be SE or TE",
                    HttpStatus.BAD_REQUEST
            );
        };
    }

    private String normalizeRegulatoryMode(String value) {
        String n = trimToNull(value);
        if (n == null) {
            return "gene_body";
        }
        String key = n.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "gene", "gene_body", "body" -> "gene_body";
            case "promoter", "prompter", "tss_promoter" -> "promoter";
            case "super_enhancer", "superenhancer", "se" -> "super_enhancer";
            case "typical_enhancer", "typicalenhancer", "enhancer", "te" -> "typical_enhancer";
            default -> throw new FeatureOccurrenceException(
                    "INVALID_PARAMETER",
                    "mode must be gene_body, promoter, super_enhancer, or typical_enhancer",
                    HttpStatus.BAD_REQUEST
            );
        };
    }

    private GeneRegion normalizeGeneRegion(GeneRegion region) {
        if (region != null && region.getStart() != null && region.getEnd() != null
                && region.getStart() > region.getEnd()) {
            long tmp = region.getStart();
            region.setStart(region.getEnd());
            region.setEnd(tmp);
        }
        return region;
    }

    private boolean isValidRegion(GeneRegion region) {
        return region != null
                && trimToNull(region.getChromosome()) != null
                && region.getStart() != null
                && region.getEnd() != null
                && region.getEnd() > region.getStart();
    }

    private boolean isValidEnhancerRegion(GeneEnhancerRegion region) {
        return region != null
                && trimToNull(region.getChromosome()) != null
                && region.getRegionStart() != null
                && region.getRegionEnd() != null
                && region.getRegionEnd() > region.getRegionStart();
    }

    private String promoterRegion(GeneRegion geneRegion) {
        if (!isValidRegion(geneRegion)) {
            return null;
        }
        boolean minusStrand = "-".equals(trimToNull(geneRegion.getStrand()));
        long tss = minusStrand ? geneRegion.getEnd() : geneRegion.getStart();
        long promoterStart = subtractToZero(tss, 2000L);
        long promoterEnd = addSafely(tss, 2000L);
        return regionString(geneRegion.getChromosome(), promoterStart, promoterEnd);
    }

    private String regionString(String chromosome, Long start, Long end) {
        String c = trimToNull(chromosome);
        if (c == null || start == null || end == null) {
            return null;
        }
        long s = start, e = end;
        if (s > e) { long t = s; s = e; e = t; }
        if (e <= s) { return null; }
        return c + ":" + s + "-" + e;
    }

    private long subtractToZero(long value, long delta) {
        return value < delta ? 0L : value - delta;
    }

    private long addSafely(long value, long delta) {
        if (value > Long.MAX_VALUE - delta) {
            return Long.MAX_VALUE;
        }
        return value + delta;
    }

    private String trimToNull(String value) { if (value == null) return null; String t = value.trim(); return t.isEmpty() ? null : t; }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class DatasetStats {
        private String sampleName;
        private long recordCount;
        private final Set<String> cellTypes = new HashSet<>();
        private final Set<String> clusters = new HashSet<>();
    }

    private static class CellContextStats {
        private long recordCount;
        private final Set<String> datasetIds = new HashSet<>();
        private final Set<String> clusters = new HashSet<>();
    }

    private record GeneOccurrenceSummary(
            int totalOccurrences,
            int datasetCount,
            int cellTypeCount,
            int clusterCount,
            List<FeatureOccurrenceTopCellType> topCellTypes,
            List<FeatureOccurrenceDatasetEntry> datasets,
            List<DatasetRankingItem> datasetRanking,
            List<CellContextRankingItem> cellContextRanking
    ) {}

    private record QueryTargetPage(List<QueryTarget> targets) {}
    private record QueryTarget(String displayRegion, String bedRegion, String enhancerRegion, String cellType) {}
}
