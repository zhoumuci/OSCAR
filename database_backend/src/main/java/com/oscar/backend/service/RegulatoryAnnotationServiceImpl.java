package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryAnnotationRecord;
import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.entity.RegulatoryAnnotationRow;
import com.oscar.backend.entity.RegulatoryTfSummaryResponse;
import com.oscar.backend.mapper.RegulatoryAnnotationMapper;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegulatoryAnnotationServiceImpl implements RegulatoryAnnotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatoryAnnotationServiceImpl.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int HARD_PAGE_SIZE = 100;
    private static final Pattern PEAK_REGION_PATTERN = Pattern.compile(
            "^([^:\\s]+)\\s*:\\s*(\\d[\\d,]*)\\s*-\\s*(\\d[\\d,]*)$"
    );

    private final RegulatoryAnnotationMapper regulatoryAnnotationMapper;
    private final RegulatoryAnnotationCountCache countCache;

    public RegulatoryAnnotationServiceImpl(
            RegulatoryAnnotationMapper regulatoryAnnotationMapper,
            RegulatoryAnnotationCountCache countCache
    ) {
        this.regulatoryAnnotationMapper = regulatoryAnnotationMapper;
        this.countCache = countCache;
    }

    @Override
    public RegulatoryAnnotationResponse getRegulatoryAnnotations(
            String datasetId,
            String domain,
            String annotationType,
            Integer page,
            Integer pageSize,
            String targetGene,
            String peak,
            String regionType,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            Double minP2gScore,
            String signalType,
            String sortBy,
            String sortOrder,
            String p2gMode
    ) {
        long overallStartedAt = System.nanoTime();
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        AnnotationType normalizedAnnotationType = normalizeAnnotationType(annotationType, normalizedDomain);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String annotationOrderBy = normalizeAnnotationOrderBy(
                normalizedAnnotationType, sortBy, sortOrder, false);
        String fallbackOrderBy = normalizeAnnotationOrderBy(
                normalizedAnnotationType, sortBy, sortOrder, true);
        ensureVisibleSampleExists(normalizedDatasetId);

        RegionFilter normalizedRegionType = normalizeRegionType(regionType);
        RegionFilter effectiveRegionType = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? RegionFilter.ALL
                : normalizedRegionType;
        if (!supportsRegionType(normalizedAnnotationType, effectiveRegionType)) {
            logQuery(
                    normalizedAnnotationType,
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedDomain,
                    normalizedPage,
                    normalizedPageSize,
                    0L,
                    0L,
                    0L,
                    0L,
                    elapsedMillis(overallStartedAt),
                    "unsupported_region_type",
                    false
            );
            return emptyResponse(normalizedPage, normalizedPageSize);
        }

        Double normalizedMaxFdr = normalizeOptionalDouble(maxFdr);
        Double normalizedMinLog2fc = normalizeMinLog2fc(minLog2fc);
        Double normalizedMinP2gScore = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? normalizeMinimumScore(minP2gScore)
                : null;
        String countRegionType = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? "p2g_link_id"
                : effectiveRegionType.name().toLowerCase(Locale.ROOT);
        String normalizedContextCellType = normalizeContextCellType(normalizedDomain, contextCellType);
        FilterPatterns filters = buildFilterPatterns(
                normalizedAnnotationType, targetGene, peak, normalizedContextCellType, contextCluster);
        PeakSearchFilter markerPeakFilter = normalizedAnnotationType == AnnotationType.MARKER_PEAK
                ? parsePeakSearchFilter(filters.peakQuery())
                : PeakSearchFilter.empty();
        PeakSearchFilter p2gPeakFilter = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? parsePeakSearchFilter(peak)
                : PeakSearchFilter.empty();
        String countPeakKey = switch (normalizedAnnotationType) {
            case MARKER_PEAK -> markerPeakFilter.cacheKey();
            case LINKED_REGION -> p2gPeakFilter.cacheKey();
            default -> filters.peakQuery();
        };
        CountFilters countFilters = new CountFilters(
                filters.targetGene(),
                countPeakKey,
                filters.contextCellType(),
                filters.contextCluster(),
                countRegionType,
                normalizedMaxFdr,
                normalizedMinLog2fc,
                normalizedMinP2gScore
        );
        long offset = pageOffset(normalizedPage, normalizedPageSize);
        PagedRows pageRows = switch (normalizedAnnotationType) {
            case MARKER_GENE -> selectMarkerGeneRows(
                    normalizedDatasetId,
                    normalizedDomain,
                    filters.targetGene(),
                    filters.contextCellType(),
                    filters.contextCluster(),
                    normalizedMaxFdr,
                    normalizedMinLog2fc,
                    countFilters,
                    normalizedPageSize,
                    offset,
                    normalizeSignalType(signalType),
                    annotationOrderBy
            );
            case MARKER_PEAK -> selectMarkerPeakRows(
                    normalizedDatasetId,
                    normalizedDomain,
                    filters.targetGene(),
                    markerPeakFilter,
                    filters.contextCellType(),
                    filters.contextCluster(),
                    normalizedMaxFdr,
                    normalizedMinLog2fc,
                    countFilters,
                    normalizedPageSize,
                    offset,
                    annotationOrderBy
            );
            case LINKED_REGION -> selectLinkedRegionRows(
                    normalizedDatasetId,
                    normalizedDomain,
                    filters.targetGene(),
                    p2gPeakFilter,
                    filters.contextCellType(),
                    filters.contextCluster(),
                    normalizedMinP2gScore,
                    countFilters,
                    normalizedPageSize,
                    offset,
                    annotationOrderBy,
                    fallbackOrderBy,
                    p2gMode
            );
        };

        long hydrateStartedAt = System.nanoTime();
        prepareRowCellTypes(pageRows.rows(), normalizedDomain);
        List<RegulatoryAnnotationRecord> records = new ArrayList<>();
        pageRows.rows()
                .stream()
                .map(row -> toRecord(normalizedAnnotationType, effectiveRegionType, row))
                .filter(Objects::nonNull)
                .forEach(record -> {
                    record.setDomain(normalizedAnnotationType == AnnotationType.LINKED_REGION
                            ? pageRows.dataDomain()
                            : normalizedDomain);
                    records.add(record);
                });
        long hydrateMillis = pageRows.hydrateMillis() + elapsedMillis(hydrateStartedAt);

        logQuery(
                normalizedAnnotationType,
                normalizedDatasetId,
                normalizedDomain,
                pageRows.dataDomain(),
                normalizedPage,
                normalizedPageSize,
                pageRows.total(),
                pageRows.countMillis(),
                pageRows.pageQueryMillis(),
                hydrateMillis,
                elapsedMillis(overallStartedAt),
                pageRows.source(),
                pageRows.countCacheHit()
        );
        return new RegulatoryAnnotationResponse(pageRows.total(), normalizedPage, normalizedPageSize, records);
    }

    @Override
    @Transactional(readOnly = true)
    public void streamRegulatoryAnnotationsCsv(
            String datasetId,
            String domain,
            String annotationType,
            String targetGene,
            String peak,
            String regionType,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            Double minP2gScore,
            String signalType,
            String sortBy,
            String sortOrder,
            String p2gMode,
            String sampleLabel,
            OutputStream outputStream
    ) throws IOException {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        AnnotationType normalizedAnnotationType = normalizeAnnotationType(annotationType, normalizedDomain);
        ensureVisibleSampleExists(normalizedDatasetId);

        RegionFilter normalizedRegionType = normalizeRegionType(regionType);
        RegionFilter effectiveRegionType = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? RegionFilter.ALL
                : normalizedRegionType;
        Double normalizedMaxFdr = normalizeOptionalDouble(maxFdr);
        Double normalizedMinLog2fc = normalizeMinLog2fc(minLog2fc);
        Double normalizedMinP2gScore = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? normalizeMinimumScore(minP2gScore)
                : null;
        String normalizedContextCellType = normalizeContextCellType(normalizedDomain, contextCellType);
        FilterPatterns filters = buildFilterPatterns(
                normalizedAnnotationType, targetGene, peak, normalizedContextCellType, contextCluster);
        PeakSearchFilter markerPeakFilter = normalizedAnnotationType == AnnotationType.MARKER_PEAK
                ? parsePeakSearchFilter(filters.peakQuery())
                : PeakSearchFilter.empty();
        PeakSearchFilter p2gPeakFilter = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? parsePeakSearchFilter(peak)
                : PeakSearchFilter.empty();
        String annotationOrderBy = normalizeAnnotationOrderBy(
                normalizedAnnotationType, sortBy, sortOrder, false);
        String fallbackOrderBy = normalizeAnnotationOrderBy(
                normalizedAnnotationType, sortBy, sortOrder, true);
        String normalizedP2gMode = "all".equalsIgnoreCase(p2gMode) ? "all" : "marker";
        String sourceLabel = normalizedDatasetId + " / " + firstNonBlank(
                trimToNull(sampleLabel), domainDisplayName(normalizedDomain));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), 64 * 1024);
        writeCsvHeader(writer, normalizedAnnotationType, normalizedP2gMode, normalizedDomain);
        writer.flush();

        if (!supportsRegionType(normalizedAnnotationType, effectiveRegionType)) {
            return;
        }

        try (Cursor<RegulatoryAnnotationRow> cursor = switch (normalizedAnnotationType) {
            case MARKER_GENE -> regulatoryAnnotationMapper.streamMarkerGenes(
                    normalizedDatasetId, normalizedDomain, filters.targetGene(),
                    filters.contextCellType(), filters.contextCluster(), normalizedMaxFdr,
                    normalizedMinLog2fc, normalizeSignalType(signalType), annotationOrderBy);
            case MARKER_PEAK -> regulatoryAnnotationMapper.streamMarkerPeaks(
                    normalizedDatasetId, normalizedDomain, filters.targetGene(),
                    markerPeakFilter.exactPeak(), markerPeakFilter.chromosome(), markerPeakFilter.start(),
                    markerPeakFilter.end(), filters.contextCellType(), filters.contextCluster(),
                    normalizedMaxFdr, normalizedMinLog2fc, annotationOrderBy);
            case LINKED_REGION -> "all".equals(normalizedP2gMode)
                    ? regulatoryAnnotationMapper.streamP2gDirect(
                            normalizedDatasetId, normalizedDomain, filters.targetGene(),
                            p2gPeakFilter.exactPeak(), p2gPeakFilter.chromosome(), p2gPeakFilter.start(),
                            p2gPeakFilter.end(), filters.contextCellType(), filters.contextCluster(),
                            normalizedMinP2gScore, fallbackOrderBy)
                    : regulatoryAnnotationMapper.streamLinkedRegions(
                            normalizedDatasetId, normalizedDomain, filters.targetGene(),
                            p2gPeakFilter.exactPeak(), p2gPeakFilter.chromosome(), p2gPeakFilter.start(),
                            p2gPeakFilter.end(), filters.contextCellType(), filters.contextCluster(),
                            normalizedMinP2gScore, annotationOrderBy);
        }) {
            int writtenRows = 0;
            for (RegulatoryAnnotationRow row : cursor) {
                RegulatoryAnnotationRecord record = toRecord(normalizedAnnotationType, effectiveRegionType, row);
                if (record == null) continue;
                record.setDomain(normalizedDomain);
                writeCsvRecord(writer, normalizedAnnotationType, normalizedP2gMode, normalizedDomain, sourceLabel, record);
                if (++writtenRows % 1_000 == 0) writer.flush();
            }
        }
        writer.flush();
    }

    private void writeCsvHeader(
            BufferedWriter writer,
            AnnotationType annotationType,
            String p2gMode,
            String domain
    ) throws IOException {
        String contextHeader = "integration".equals(domain) ? "Cell / Cluster" : "Cluster";
        switch (annotationType) {
            case MARKER_GENE -> writeCsvRow(writer,
                    "Gene", contextHeader, "Gene region", "Promoter region", "Strand",
                    "Log2FC", "FDR", "MeanDiff", "Sample");
            case MARKER_PEAK -> writeCsvRow(writer,
                    "Peak", contextHeader, "Linked gene", "Log2FC", "FDR", "MeanDiff", "Sample");
            case LINKED_REGION -> {
                if ("all".equals(p2gMode)) {
                    writeCsvRow(writer,
                            "Gene", "Linked peak", "P2G score", "FDR", "VarQ RNA", "VarQ ATAC", "Sample");
                } else {
                    writeCsvRow(writer,
                            "Gene", contextHeader, "Linked peak", "P2G score", "Gene Diff",
                            "Peak Diff", "Gene marker type", "Sample");
                }
            }
        }
    }

    private void writeCsvRecord(
            BufferedWriter writer,
            AnnotationType annotationType,
            String p2gMode,
            String domain,
            String sourceLabel,
            RegulatoryAnnotationRecord record
    ) throws IOException {
        switch (annotationType) {
            case MARKER_GENE -> writeCsvRow(writer,
                    record.getTargetGene(), csvContext(record, domain), record.getGeneRegion(),
                    record.getPromoterRegion(), record.getStrand(), csvMetric(record.getGeneLog2fc()),
                    csvMetric(record.getGeneFdr()), csvMetric(record.getGeneMeanDiff()), sourceLabel);
            case MARKER_PEAK -> writeCsvRow(writer,
                    firstNonBlank(record.getPeakRegion(), record.getPeakName()), csvContext(record, domain),
                    record.getLinkedGene(), csvMetric(record.getPeakLog2fc()), csvMetric(record.getPeakFdr()),
                    csvMetric(record.getPeakMeanDiff()), sourceLabel);
            case LINKED_REGION -> {
                String linkedPeak = firstNonBlank(record.getLinkedPeak(), record.getPeakRegion(), record.getPeakName());
                if ("all".equals(p2gMode)) {
                    writeCsvRow(writer,
                            record.getTargetGene(), linkedPeak, csvMetric(record.getLinkScore()),
                            csvMetric(record.getLinkFdr()), csvMetric(record.getVarQrna()),
                            csvMetric(record.getVarQatac()), sourceLabel);
                } else {
                    writeCsvRow(writer,
                            record.getTargetGene(), csvContext(record, domain), linkedPeak,
                            csvMetric(record.getLinkScore()), csvMarkerEvidence(record.getGeneLog2fc(), record.getGeneFdr()),
                            csvMarkerEvidence(record.getPeakLog2fc(), record.getPeakFdr()),
                            firstNonBlank(record.getSignalType(), "-"), sourceLabel);
                }
            }
        }
    }

    private String csvContext(RegulatoryAnnotationRecord record, String domain) {
        String cluster = trimToNull(record.getClusterLabel());
        if (!"integration".equals(domain)) return firstNonBlank(cluster, "-");
        String cellType = trimToNull(record.getCellType());
        if (cellType == null) return firstNonBlank(cluster, "-");
        if (cluster == null || cellType.equalsIgnoreCase(cluster)) return cellType;
        return cellType + " / " + cluster;
    }

    private String csvMarkerEvidence(Double log2fc, Double fdr) {
        List<String> values = new ArrayList<>(2);
        if (log2fc != null) values.add("L2FC " + csvMetric(log2fc));
        if (fdr != null) values.add("FDR " + csvMetric(fdr));
        return values.isEmpty() ? "-" : String.join(" / ", values);
    }

    private String csvMetric(Double value) {
        return value == null ? "-" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void writeCsvRow(BufferedWriter writer, Object... values) throws IOException {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) writer.write(',');
            String value = values[index] == null ? "" : String.valueOf(values[index]);
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write('\n');
    }

    private String domainDisplayName(String domain) {
        return switch (domain) {
            case "rna" -> "RNA";
            case "atac" -> "ATAC";
            case "integration" -> "Integration";
            default -> domain;
        };
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "contextOptions", key = "#datasetId + ':' + #domain + ':' + #annotationType")
    public List<RegulatoryAnnotationContextOption> getRegulatoryAnnotationContextOptions(
            String datasetId,
            String domain,
            String annotationType
    ) {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        AnnotationType normalizedAnnotationType = normalizeAnnotationType(annotationType, normalizedDomain);
        ensureVisibleSampleExists(normalizedDatasetId);

        List<RegulatoryAnnotationContextOption> options = switch (normalizedAnnotationType) {
            case MARKER_GENE -> regulatoryAnnotationMapper.selectMarkerGeneContextOptions(normalizedDatasetId, normalizedDomain);
            case MARKER_PEAK -> regulatoryAnnotationMapper.selectMarkerPeakContextOptions(normalizedDatasetId, normalizedDomain);
            case LINKED_REGION -> regulatoryAnnotationMapper.selectLinkedRegionContextOptions(
                    normalizedDatasetId,
                    normalizedDomain
            );
        };
        return normalizeContextOptions(options, normalizedDomain);
    }

    @Override
    public RegulatoryTfSummaryResponse getTfSummary(
            String datasetId,
            String domain,
            String featureType,
            String gene,
            String chrom,
            Long start,
            Long end,
            String peakId
    ) {
        String normalizedDatasetId = trimToNull(datasetId);
        if (normalizedDatasetId == null) {
            normalizedDatasetId = "";
        }
        String normalizedDomain = trimToNull(domain);
        if (normalizedDomain == null) {
            normalizedDomain = "integration";
        }

        String normalizedFeatureType = trimToNull(featureType);
        if (normalizedFeatureType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "featureType must be gene or peak");
        }
        normalizedFeatureType = normalizedFeatureType.toLowerCase(Locale.ROOT);

        if ("gene".equals(normalizedFeatureType)) {
            String normalizedGene = trimToNull(gene);
            if (normalizedGene == null) {
                return tfUnavailable(
                        normalizedDatasetId,
                        normalizedDomain,
                        "gene",
                        null,
                        "Gene symbol is required to look up TF annotation."
                );
            }
            return tfUnavailable(
                    normalizedDatasetId,
                    normalizedDomain,
                    "gene",
                    normalizedGene,
                    "TF annotation data have not been integrated yet."
            );
        }

        if ("peak".equals(normalizedFeatureType)) {
            String normalizedPeakId = trimToNull(peakId);
            String normalizedChrom = trimToNull(chrom);
            String featureId = normalizedPeakId != null
                    ? normalizedPeakId
                    : peakFeatureId(normalizedChrom, start, end);
            if (normalizedChrom == null || start == null || end == null) {
                return tfUnavailable(
                        normalizedDatasetId,
                        normalizedDomain,
                        "peak",
                        featureId,
                        "Peak chromosome, start, and end are required to look up TF annotation."
                );
            }
            return tfUnavailable(
                    normalizedDatasetId,
                    normalizedDomain,
                    "peak",
                    featureId,
                    "TF annotation data have not been integrated yet."
            );
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "featureType must be gene or peak");
    }

    private PagedRows selectMarkerGeneRows(
            String datasetId,
            String domain,
            String targetGene,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            CountFilters countFilters,
            int limit,
            long offset,
            String signalType,
            String orderBy
    ) {
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.MARKER_GENE,
                datasetId,
                domain,
                countFilters,
                signalType,
                () -> regulatoryAnnotationMapper.countMarkerGenes(
                        datasetId,
                        domain,
                        targetGene,
                        contextCellType,
                        contextCluster,
                        maxFdr,
                        minLog2fc,
                        signalType
                )
        );
        long total = countResult.total();
        String dataDomain = domain;
        long countMillis = countResult.countMillis();
        boolean countCacheHit = countResult.cacheHit();
        if (total == 0L || offset >= total) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, 0L, 0L, "marker_gene", countCacheHit);
        }
        long pageStartedAt = System.nanoTime();
        List<RegulatoryAnnotationRow> rows = regulatoryAnnotationMapper.selectMarkerGenes(
                datasetId,
                dataDomain,
                targetGene,
                contextCellType,
                contextCluster,
                maxFdr,
                minLog2fc,
                signalType,
                orderBy,
                limit,
                intOffset(offset)
        );
        return new PagedRows(dataDomain, total, rows, countMillis, elapsedMillis(pageStartedAt), 0L, "marker_gene", countCacheHit);
    }

    private PagedRows selectMarkerPeakRows(
            String datasetId,
            String domain,
            String targetGene,
            PeakSearchFilter peakFilter,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            CountFilters countFilters,
            int limit,
            long offset,
            String orderBy
    ) {
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.MARKER_PEAK,
                datasetId,
                domain,
                countFilters,
                null,
                () -> regulatoryAnnotationMapper.countMarkerPeaks(
                        datasetId,
                        domain,
                        targetGene,
                        peakFilter.exactPeak(),
                        peakFilter.chromosome(),
                        peakFilter.start(),
                        peakFilter.end(),
                        contextCellType,
                        contextCluster,
                        maxFdr,
                        minLog2fc
                )
        );
        long total = countResult.total();
        String dataDomain = domain;
        long countMillis = countResult.countMillis();
        boolean countCacheHit = countResult.cacheHit();
        if (total == 0L || offset >= total) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, 0L, 0L, "marker_peak", countCacheHit);
        }
        long pageStartedAt = System.nanoTime();
        List<Long> pageIds = regulatoryAnnotationMapper.selectMarkerPeakPageIds(
                datasetId,
                dataDomain,
                targetGene,
                peakFilter.exactPeak(),
                peakFilter.chromosome(),
                peakFilter.start(),
                peakFilter.end(),
                contextCellType,
                contextCluster,
                maxFdr,
                minLog2fc,
                orderBy,
                limit,
                intOffset(offset)
        );
        if (pageIds == null || pageIds.isEmpty()) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, elapsedMillis(pageStartedAt), 0L, "marker_peak", countCacheHit);
        }
        List<RegulatoryAnnotationRow> rows = regulatoryAnnotationMapper.selectMarkerPeaksByIds(pageIds, targetGene);
        long pageQueryMillis = elapsedMillis(pageStartedAt);
        return new PagedRows(dataDomain, total, rows, countMillis, pageQueryMillis, 0L, "marker_peak", countCacheHit);
    }

    private PagedRows selectLinkedRegionRows(
            String datasetId,
            String domain,
            String targetGene,
            PeakSearchFilter peakFilter,
            String contextCellType,
            String contextCluster,
            Double minP2gScore,
            CountFilters countFilters,
            int limit,
            long offset,
            String orderBy,
            String fallbackOrderBy,
            String p2gMode
    ) {
        boolean useP2gAll = "all".equalsIgnoreCase(p2gMode);
        String dataDomain = domain;
        if (useP2gAll) {
            return selectP2gFallbackRows(
                    datasetId,
                    dataDomain,
                    targetGene,
                    peakFilter,
                    contextCellType,
                    contextCluster,
                    minP2gScore,
                    limit,
                    offset,
                    fallbackOrderBy
            );
        }
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.LINKED_REGION,
                datasetId,
                dataDomain,
                countFilters,
                null,
                () -> regulatoryAnnotationMapper.countLinkedRegions(
                        datasetId,
                        dataDomain,
                        targetGene,
                        peakFilter.exactPeak(),
                        peakFilter.chromosome(),
                        peakFilter.start(),
                        peakFilter.end(),
                        contextCellType,
                        contextCluster,
                        minP2gScore
                )
        );
        long total = countResult.total();
        long countMillis = countResult.countMillis();
        boolean countCacheHit = countResult.cacheHit();
        if (total == 0L || offset >= total) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, 0L, 0L, "materialized_linked_region", countCacheHit);
        }
        long pageStartedAt = System.nanoTime();
        List<Long> pageIds = regulatoryAnnotationMapper.selectLinkedRegionPageIds(
                datasetId,
                dataDomain,
                targetGene,
                peakFilter.exactPeak(),
                peakFilter.chromosome(),
                peakFilter.start(),
                peakFilter.end(),
                contextCellType,
                contextCluster,
                minP2gScore,
                orderBy,
                limit,
                intOffset(offset)
        );
        if (pageIds == null || pageIds.isEmpty()) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, elapsedMillis(pageStartedAt), 0L, "materialized_linked_region", countCacheHit);
        }
        List<RegulatoryAnnotationRow> rows = regulatoryAnnotationMapper.selectLinkedRegionsByIds(pageIds);
        return new PagedRows(
                dataDomain,
                total,
                rows,
                countMillis,
                elapsedMillis(pageStartedAt),
                0L,
                "materialized_linked_region",
                countCacheHit
        );
    }

    private void mergeMarkerPeakLinkSummaries(String datasetId, String dataDomain, String targetGene, List<RegulatoryAnnotationRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        String normalizedGene = targetGene != null ? targetGene.toUpperCase().trim() : null;
        List<RegulatoryAnnotationRow> linkSummaries = regulatoryAnnotationMapper.selectMarkerPeakLinkSummaries(
                datasetId,
                dataDomain,
                rows,
                normalizedGene
        );
        if (linkSummaries == null || linkSummaries.isEmpty()) {
            return;
        }

        Map<Long, RegulatoryAnnotationRow> summaryByPeakId = new HashMap<>();
        for (RegulatoryAnnotationRow summary : linkSummaries) {
            if (summary.getMarkerPeakId() != null) {
                summaryByPeakId.put(summary.getMarkerPeakId(), summary);
            }
        }
        for (RegulatoryAnnotationRow row : rows) {
            RegulatoryAnnotationRow summary = summaryByPeakId.get(row.getMarkerPeakId());
            if (summary == null) {
                continue;
            }
            row.setLinkedGeneName(summary.getLinkedGeneName());
            row.setLinkedGeneCount(summary.getLinkedGeneCount());
            row.setLinkScore(summary.getLinkScore());
            row.setCorrelation(summary.getCorrelation());
            row.setLinkFdr(summary.getLinkFdr());
            row.setLinkSource(summary.getLinkSource());
        }
    }

    private PagedRows selectP2gFallbackRows(
            String datasetId,
            String domain,
            String targetGene,
            PeakSearchFilter peakFilter,
            String contextCellType,
            String contextCluster,
            Double minP2gScore,
            int limit,
            long offset,
            String orderBy
    ) {
        long countStartedAt = System.nanoTime();
        String dataDomain = domain;
        String normalizedGene = targetGene != null ? targetGene.toUpperCase().trim() : null;
        long total = regulatoryAnnotationMapper.countP2gDirect(
                datasetId,
                dataDomain,
                normalizedGene,
                peakFilter.exactPeak(),
                peakFilter.chromosome(),
                peakFilter.start(),
                peakFilter.end(),
                contextCellType,
                contextCluster,
                minP2gScore
        );
        long countMillis = (System.nanoTime() - countStartedAt) / 1_000_000;

        if (total == 0L || offset >= total) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, 0L, 0L, "p2g_fallback", false);
        }
        long pageStartedAt = System.nanoTime();
        List<Long> pageIds = regulatoryAnnotationMapper.selectP2gDirectPageIds(
                datasetId,
                dataDomain,
                normalizedGene,
                peakFilter.exactPeak(),
                peakFilter.chromosome(),
                peakFilter.start(),
                peakFilter.end(),
                contextCellType,
                contextCluster,
                minP2gScore,
                orderBy,
                limit,
                (int) offset
        );
        if (pageIds == null || pageIds.isEmpty()) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, (System.nanoTime() - pageStartedAt) / 1_000_000, 0L, "p2g_fallback", false);
        }
        List<RegulatoryAnnotationRow> rows = regulatoryAnnotationMapper.selectP2gDirectByIds(pageIds);
        return new PagedRows(dataDomain, total, rows, countMillis,
                (System.nanoTime() - pageStartedAt) / 1_000_000, 0L, "p2g_fallback", false);
    }

    private RegulatoryAnnotationCountCache.CountResult cachedCount(
            AnnotationType annotationType,
            String datasetId,
            String dataDomain,
            CountFilters filters,
            String signalType,
            java.util.function.LongSupplier loader
    ) {
        return countCache.getOrLoad(
                new RegulatoryAnnotationCountCache.CountCacheKey(
                        annotationType.value(),
                        datasetId,
                        dataDomain,
                        filters.targetGene(),
                        filters.peak(),
                        filters.contextCellType(),
                        filters.contextCluster(),
                        filters.regionType(),
                        filters.maxFdr(),
                        filters.minLog2fc(),
                        filters.minP2gScore(),
                        signalType
                ),
                loader
        );
    }

    private RegulatoryAnnotationRecord toRecord(
            AnnotationType annotationType,
            RegionFilter requestedRegionType,
            RegulatoryAnnotationRow row
    ) {
        return switch (annotationType) {
            case MARKER_GENE -> toMarkerGeneRecord(row);
            case MARKER_PEAK -> toMarkerPeakRecord(row, requestedRegionType);
            case LINKED_REGION -> toLinkedRegionRecord(row);
        };
    }

    private RegulatoryAnnotationRecord toMarkerGeneRecord(RegulatoryAnnotationRow row) {
        String gene = trimToNull(row.getGeneSymbol());
        if (gene == null) {
            return null;
        }

        Long normalizedGeneStart = normStart(row.getGeneStart(), row.getGeneEnd());
        Long normalizedGeneEnd = normEnd(row.getGeneStart(), row.getGeneEnd());

        PromoterRegion promoter = calculatePromoterRegion(
                row.getGeneChromosome(),
                normalizedGeneStart,
                normalizedGeneEnd,
                row.getStrand()
        );
        String normalizedStrand = normalizeStrand(row.getStrand());
        String geneRegion = regionString(row.getGeneChromosome(), normalizedGeneStart, normalizedGeneEnd);
        String promoterRegion = regionString(promoter.chromosome(), promoter.start(), promoter.end());

        RegulatoryAnnotationRecord record = baseRecord(row, AnnotationType.MARKER_GENE.value());
        record.setSignalType(trimToEmpty(row.getSignalType()));
        record.setId("marker-gene-" + row.getMarkerGeneId());
        record.setTargetGene(gene);
        record.setGeneSymbol(gene);
        record.setGene(gene);
        record.setGeneId(trimToEmpty(row.getGeneId()));
        record.setGeneChromosome(trimToEmpty(row.getGeneChromosome()));
        record.setGeneStart(normalizedGeneStart);
        record.setGeneEnd(normalizedGeneEnd);
        record.setGeneRegion(geneRegion);
        record.setStrand(normalizedStrand);
        record.setPromoterRegion(promoterRegion);
        record.setGeneLog2fc(row.getAvgLog2fc());
        record.setGeneFdr(row.getGeneFdr());
        record.setGeneMeanDiff(row.getGeneMeanDiff());
        record.setRegionType("promoter");
        record.setChromosome(promoter.chromosome());
        record.setStart(promoter.start());
        record.setEnd(promoter.end());
        record.setRegion(promoterRegion);
        record.setLog2fc(row.getAvgLog2fc());
        record.setLogFc(row.getAvgLog2fc());
        record.setFdr(row.getGeneFdr());
        record.setMeanDiff(row.getGeneMeanDiff());
        record.setAdjustedPValue(row.getGeneFdr());
        record.setSource(firstNonBlank(row.getMarkerGeneSourceFile(), "marker_gene_exp"));
        record.setEvidence("RNA marker gene; " + metricText(
                "Log2FC", row.getAvgLog2fc(),
                "FDR", row.getGeneFdr(),
                "MeanDiff", row.getGeneMeanDiff()
        ));
        return record;
    }

    private RegulatoryAnnotationRecord toMarkerPeakRecord(RegulatoryAnnotationRow row, RegionFilter requestedRegionType) {
        String peakRegion = regionString(row.getPeakChromosome(), row.getPeakStart(), row.getPeakEnd());
        String peakName = firstNonBlank(peakRegion, row.getPeakName());
        String linkedGene = trimToEmpty(row.getLinkedGeneName());
        String outputRegionType = peakRegionType(row, requestedRegionType);

        RegulatoryAnnotationRecord record = baseRecord(row, AnnotationType.MARKER_PEAK.value());
        record.setId(markerPeakId(row));
        record.setTargetGene(linkedGene);
        record.setPeakName(peakName);
        record.setPeak(peakName);
        record.setPeakId(peakName);
        record.setPeakChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setPeakStart(normStart(row.getPeakStart(), row.getPeakEnd()));
        record.setPeakEnd(normEnd(row.getPeakStart(), row.getPeakEnd()));
        record.setPeakRegion(peakRegion);
        record.setPeakLog2fc(row.getPeakLog2fc());
        record.setPeakFdr(row.getPeakFdr());
        record.setPeakMeanDiff(row.getPeakMeanDiff());
        record.setLinkedGene(linkedGene);
        record.setLinkedGeneCount(row.getLinkedGeneCount());
        record.setLinkScore(row.getLinkScore());
        record.setCorrelation(row.getCorrelation());
        record.setLinkFdr(row.getLinkFdr());
        record.setRegionType(outputRegionType);
        record.setRegulatoryRegion(peakRegion);
        record.setChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setStart(normStart(row.getPeakStart(), row.getPeakEnd()));
        record.setEnd(normEnd(row.getPeakStart(), row.getPeakEnd()));
        record.setRegion(peakRegion);
        record.setLog2fc(row.getPeakLog2fc());
        record.setLogFc(row.getPeakLog2fc());
        record.setFdr(row.getPeakFdr());
        record.setMeanDiff(row.getPeakMeanDiff());
        record.setAdjustedPValue(row.getPeakFdr());
        record.setSource(firstNonBlank(row.getMarkerPeakSourceFile(), "marker_peak"));
        record.setEvidence(markerPeakEvidence(row, linkedGene));
        return record;
    }

    private RegulatoryAnnotationRecord toLinkedRegionRecord(RegulatoryAnnotationRow row) {
        String gene = trimToNull(row.getGeneSymbol());
        if (gene == null || row.getPeakGeneLinkId() == null) {
            return null;
        }

        Long normalizedGeneStart = normStart(row.getGeneStart(), row.getGeneEnd());
        Long normalizedGeneEnd = normEnd(row.getGeneStart(), row.getGeneEnd());

        String geneRegion = regionString(row.getGeneChromosome(), normalizedGeneStart, normalizedGeneEnd);
        PromoterRegion promoter = calculatePromoterRegion(
                row.getGeneChromosome(),
                normalizedGeneStart,
                normalizedGeneEnd,
                row.getStrand()
        );
        String normalizedStrand = normalizeStrand(row.getStrand());
        String promoterRegion = regionString(promoter.chromosome(), promoter.start(), promoter.end());
        String peakRegion = firstNonBlank(
                row.getPeakRegion(),
                regionString(row.getPeakChromosome(), row.getPeakStart(), row.getPeakEnd())
        );
        String peakName = firstNonBlank(peakRegion, trimToEmpty(row.getPeakName()));
        String linkedPeak = firstNonBlank(peakRegion, row.getPeakName());
        boolean hasMarkerPeak = row.getMarkerPeakId() != null;

        RegulatoryAnnotationRecord record = baseRecord(row, AnnotationType.LINKED_REGION.value());
        record.setId("linked-region-" + firstNonBlank(
                row.getLinkedRegionId() == null ? null : row.getLinkedRegionId().toString(),
                row.getMarkerGeneId() + "-" + row.getPeakGeneLinkId()
        ));
        record.setContext(linkedRegionContext(row));
        record.setTargetGene(gene);
        record.setGeneSymbol(gene);
        record.setGene(gene);
        record.setGeneId(trimToEmpty(row.getGeneId()));
        record.setGeneChromosome(trimToEmpty(row.getGeneChromosome()));
        record.setGeneStart(normalizedGeneStart);
        record.setGeneEnd(normalizedGeneEnd);
        record.setGeneRegion(geneRegion);
        record.setStrand(normalizedStrand);
        record.setPromoterRegion(promoterRegion);
        record.setGeneLog2fc(row.getAvgLog2fc());
        record.setGeneFdr(row.getGeneFdr());
        record.setGeneMeanDiff(row.getGeneMeanDiff());
        record.setPeakName(peakName);
        record.setPeak(peakName);
        record.setPeakId(linkedPeak);
        record.setPeakChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setPeakStart(normStart(row.getPeakStart(), row.getPeakEnd()));
        record.setPeakEnd(normEnd(row.getPeakStart(), row.getPeakEnd()));
        record.setPeakRegion(peakRegion);
        record.setPeakLog2fc(row.getPeakLog2fc());
        record.setPeakFdr(row.getPeakFdr());
        record.setPeakMeanDiff(row.getPeakMeanDiff());
        record.setLinkedPeak(linkedPeak);
        record.setLinkedGene(firstNonBlank(row.getLinkedGeneName(), gene));
        record.setLinkScore(row.getLinkScore());
        record.setCorrelation(row.getCorrelation());
        record.setLinkFdr(row.getLinkFdr());
        record.setSignalType(trimToNull(row.getSignalType()));
        record.setVarQrna(row.getVarQrna());
        record.setVarQatac(row.getVarQatac());
        record.setDistance(distanceToTss(normalizedGeneStart, normalizedGeneEnd, row));
        record.setRegulatoryRegion(peakRegion);
        record.setChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setStart(normStart(row.getPeakStart(), row.getPeakEnd()));
        record.setEnd(normEnd(row.getPeakStart(), row.getPeakEnd()));
        record.setRegion(peakRegion);
        record.setFdr(row.getLinkFdr());
        record.setSource(firstNonBlank(row.getLinkSource(), hasMarkerPeak
                ? "materialized_marker_gene_peak_link"
                : "materialized_marker_gene_link"));
        record.setEvidence(linkedRegionEvidence(row, hasMarkerPeak));
        return record;
    }

    private RegulatoryAnnotationRecord baseRecord(RegulatoryAnnotationRow row, String annotationType) {
        String clusterLabel = firstNonBlank(row.getClusterLabel(), row.getGroupName());
        String cellType = "integration".equalsIgnoreCase(trimToEmpty(row.getDomain()))
                ? trimToNull(row.getCellType())
                : null;
        String context = cellType == null
                ? clusterLabel
                : (trimToNull(clusterLabel) == null ? cellType : cellType + " (" + clusterLabel + ")");

        RegulatoryAnnotationRecord record = new RegulatoryAnnotationRecord();
        record.setAnnotationType(annotationType);
        record.setDatasetId(row.getDatasetId());
        record.setDomain(row.getDomain());
        record.setContext(context);
        record.setCellType(cellType);
        record.setClusterLabel(clusterLabel);
        record.setSource("");
        record.setEvidence("");
        record.setTargetGene("");
        record.setGeneSymbol("");
        record.setGeneId("");
        record.setGeneChromosome("");
        record.setGeneRegion("");
        record.setStrand("");
        record.setPromoterRegion("");
        record.setPeakName("");
        record.setPeakChromosome("");
        record.setPeakRegion("");
        record.setLinkedPeak("");
        record.setLinkedGene("");
        record.setRegionType("");
        record.setRegulatoryRegion("");
        record.setTf("");
        record.setMotifName("");
        record.setMotifId("");
        record.setMotifSource("");
        record.setMotifLogoUrl("");
        record.setTfbsRegion("");
        record.setPromoterTf("");
        record.setSeTf("");
        record.setTeTf("");
        record.setChromosome("");
        record.setRegion("");
        record.setPeak("");
        record.setPeakId("");
        record.setGene("");
        return record;
    }

    private FilterPatterns buildFilterPatterns(AnnotationType annotationType, String targetGene, String peak, String contextCellType, String contextCluster) {
        String normalizedTargetGene = exactGeneSymbol(targetGene);
        String normalizedPeak = trimToNull(peak);

        if (annotationType == AnnotationType.MARKER_PEAK && sameSearchTerm(targetGene, normalizedPeak)) {
            if (isGenomicRegionSearch(normalizedPeak)) {
                normalizedTargetGene = null;
            } else {
                normalizedPeak = null;
            }
        }

        return new FilterPatterns(
                normalizedTargetGene,
                normalizedPeak,
                trimToNull(contextCellType),
                trimToNull(contextCluster)
        );
    }

    private String markerPeakId(RegulatoryAnnotationRow row) {
        String id = "marker-peak-" + row.getMarkerPeakId();
        if (row.getPeakGeneLinkId() != null) {
            return id + "-" + row.getPeakGeneLinkId();
        }
        return id;
    }

    private String peakRegionType(RegulatoryAnnotationRow row, RegionFilter requestedRegionType) {
        if (requestedRegionType == RegionFilter.ENHANCER) {
            return "enhancer";
        }
        if (requestedRegionType == RegionFilter.LINKED_PEAK) {
            return "linked_peak";
        }
        return row.getPeakGeneLinkId() == null ? "enhancer" : "linked_peak";
    }

    private String markerPeakEvidence(RegulatoryAnnotationRow row, String linkedGene) {
        List<String> parts = new ArrayList<>();
        parts.add("ATAC marker peak; " + metricText(
                "Log2FC", row.getPeakLog2fc(),
                "FDR", row.getPeakFdr(),
                "MeanDiff", row.getPeakMeanDiff()
        ));
        if (trimToNull(linkedGene) != null || row.getLinkScore() != null) {
            parts.add("linkedGene=" + valueOrNa(linkedGene)
                    + ", linkScore=" + formatNumber(row.getLinkScore()));
        }
        return String.join("; ", parts);
    }

    private String linkedRegionEvidence(RegulatoryAnnotationRow row, boolean hasMarkerPeak) {
        return "Linked regulatory peak; linkScore=" + formatNumber(row.getLinkScore())
                + ", geneFdr=" + formatNumber(row.getGeneFdr())
                + ", peakFdr=" + formatNumber(row.getPeakFdr());
    }

    private String linkedRegionContext(RegulatoryAnnotationRow row) {
        String contextLabel = trimToNull(row.getContextLabel());
        if (contextLabel != null) {
            return contextLabel;
        }

        String groupName = firstNonBlank(row.getGroupName(), row.getClusterLabel());
        String cellType = trimToNull(row.getCellType());
        if (cellType != null) {
            return trimToNull(groupName) == null ? cellType : cellType + " / " + groupName;
        }
        return groupName;
    }

    private List<RegulatoryAnnotationContextOption> normalizeContextOptions(
            List<RegulatoryAnnotationContextOption> rawOptions,
            String domain
    ) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            return List.of();
        }

        boolean includeCellType = "integration".equals(domain);
        Map<String, RegulatoryAnnotationContextOption> optionByValue = new LinkedHashMap<>();
        for (RegulatoryAnnotationContextOption rawOption : rawOptions) {
            if (rawOption == null) {
                continue;
            }

            String cellType = includeCellType
                    ? trimToNull(rawOption.getCellType())
                    : null;
            String cluster = trimToNull(rawOption.getCluster());
            String label = contextOptionLabel(cellType, cluster);
            if (label == null) {
                continue;
            }

            String value = contextOptionValue(cellType, cluster);
            RegulatoryAnnotationContextOption option = optionByValue.computeIfAbsent(value, ignored -> {
                RegulatoryAnnotationContextOption created = new RegulatoryAnnotationContextOption();
                created.setCellType(cellType);
                created.setCluster(cluster);
                created.setLabel(label);
                created.setValue(value);
                created.setCount(0L);
                return created;
            });
            option.setCount(safeCount(option.getCount()) + safeCount(rawOption.getCount()));
        }
        return new ArrayList<>(optionByValue.values());
    }

    private void prepareRowCellTypes(List<RegulatoryAnnotationRow> rows, String domain) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if ("integration".equals(domain)) {
            rows.forEach(row -> row.setCellType(trimToNull(row.getCellType())));
            return;
        }
        rows.forEach(row -> row.setCellType(null));
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String contextOptionLabel(String cellType, String cluster) {
        if (cellType != null && cluster != null && !cellType.equalsIgnoreCase(cluster)) {
            return cellType + " / " + cluster;
        }
        return trimToNull(firstNonBlank(cellType, cluster));
    }

    private String contextOptionValue(String cellType, String cluster) {
        return trimToEmpty(cellType) + "||" + trimToEmpty(cluster);
    }

    private String metricText(String firstName, Double firstValue, String secondName, Double secondValue, String thirdName, Double thirdValue) {
        return firstName + "=" + formatNumber(firstValue)
                + ", " + secondName + "=" + formatNumber(secondValue)
                + ", " + thirdName + "=" + formatNumber(thirdValue);
    }

    private String formatNumber(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "NA";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String valueOrNa(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "NA" : normalized;
    }

    private PromoterRegion calculatePromoterRegion(String chromosome, Long geneStart, Long geneEnd, String strand) {
        String normalizedChromosome = trimToNull(chromosome);
        boolean minusStrand = "-".equals(normalizeStrand(strand));
        Long tss = minusStrand ? geneEnd : geneStart;
        if (tss == null) {
            return new PromoterRegion(normalizedChromosome, null, null);
        }

        long promoterStart = subtractToZero(tss, 2000L);
        long promoterEnd = addSafely(tss, 2000L);
        return new PromoterRegion(normalizedChromosome, promoterStart, promoterEnd);
    }

    private Long distanceToTss(Long normalizedGeneStart, Long normalizedGeneEnd, RegulatoryAnnotationRow row) {
        Long tss = "-".equals(normalizeStrand(row.getStrand())) ? normalizedGeneEnd : normalizedGeneStart;
        Long peakStart = row.getPeakStart();
        Long peakEnd = row.getPeakEnd();
        if (tss == null || peakStart == null || peakEnd == null) {
            return null;
        }
        long peakCenter = peakStart + ((peakEnd - peakStart) / 2L);
        return Math.abs(peakCenter - tss);
    }

    private String normalizeStrand(String strand) {
        String normalized = trimToNull(strand);
        if (normalized == null) {
            return null;
        }
        String key = normalized.toLowerCase(Locale.ROOT);
        if ("1".equals(key) || "+".equals(key) || "plus".equals(key) || "forward".equals(key)) {
            return "+";
        }
        if ("2".equals(key) || "-1".equals(key) || "-".equals(key) || "minus".equals(key) || "reverse".equals(key)) {
            return "-";
        }
        return null;
    }

    private boolean supportsRegionType(AnnotationType annotationType, RegionFilter regionType) {
        if (annotationType == AnnotationType.LINKED_REGION) {
            return true;
        }
        if (regionType == RegionFilter.UNSUPPORTED || regionType == RegionFilter.SUPER_ENHANCER) {
            return false;
        }
        return switch (annotationType) {
            case MARKER_GENE -> regionType == RegionFilter.ALL || regionType == RegionFilter.PROMOTER;
            case MARKER_PEAK -> regionType == RegionFilter.ALL
                    || regionType == RegionFilter.ENHANCER
                    || regionType == RegionFilter.LINKED_PEAK;
            case LINKED_REGION -> true;
        };
    }

    private void ensureVisibleSampleExists(String datasetId) {
        if (regulatoryAnnotationMapper.countVisibleSampleByDatasetId(datasetId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "datasetId not found");
        }
    }

    private String normalizeRequired(String value, String parameterName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " is required");
        }
        return normalized;
    }

    private String normalizeDomain(String domain) {
        String normalized = trimToNull(domain);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain is required");
        }

        String key = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (key.contains("integration") || key.contains("integrated") || key.contains("multiome")
                || key.contains("multiomics") || (key.contains("rna") && key.contains("atac"))) {
            return "integration";
        }
        if (key.equals("rna") || key.contains("scrna") || key.contains("rnaseq") || key.contains("geneexpression")) {
            return "rna";
        }
        if (key.equals("atac") || key.contains("scatac") || key.contains("atacseq") || key.contains("chromatin")) {
            return "atac";
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain must be integration, rna, or atac");
    }

    /** Accept "gene_score" or "gene_expression" / "gene_exp"; null/other → null (no filter). */
    private String normalizeContextCellType(String domain, String contextCellType) {
        String normalized = trimToNull(contextCellType);
        if (normalized != null && !"integration".equals(domain)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "contextCellType is only available for domain=integration; use contextCluster for rna/atac"
            );
        }
        return normalized;
    }

    private String normalizeSignalType(String signalType) {
        String v = trimToNull(signalType);
        if (v == null) return null;
        if ("gene_score".equals(v)) return "gene_score";
        if ("gene_exp".equals(v) || "gene_expression".equals(v)) return "gene_expression";
        return null;
    }

    /**
     * Converts user-facing sort keys into fixed SQL fragments. No request value is
     * interpolated directly into SQL; only these whitelisted expressions reach MyBatis.
     */
    private String normalizeAnnotationOrderBy(
            AnnotationType annotationType,
            String sortBy,
            String sortOrder,
            boolean p2gFallback
    ) {
        String key = trimToNull(sortBy);
        if (key == null) {
            return null;
        }
        String normalizedOrder = trimToNull(sortOrder);
        if (normalizedOrder == null
                || !("asc".equalsIgnoreCase(normalizedOrder) || "desc".equalsIgnoreCase(normalizedOrder))) {
            return null;
        }
        String direction = "desc".equalsIgnoreCase(normalizedOrder) ? "DESC" : "ASC";

        if (annotationType == AnnotationType.MARKER_GENE) {
            return switch (key) {
                case "targetGene" -> textOrder("mg.gene_symbol", direction, "mg.group_name ASC, mg.id ASC");
                case "context" -> textOrder(
                        "COALESCE(NULLIF(TRIM(ca.major_cell_type), ''), NULLIF(TRIM(mg.group_name), ''))",
                        direction,
                        "mg.group_name " + direction + ", mg.gene_symbol ASC, mg.id ASC");
                case "geneRegion" -> genomicOrder(
                        "mg.chromosome", "mg.gene_start", "mg.gene_end", direction, "mg.id ASC");
                case "promoterRegion" -> genomicOrder(
                        "mg.chromosome",
                        "CASE WHEN TRIM(mg.strand) = '-' THEN mg.gene_end ELSE mg.gene_start END",
                        "CASE WHEN TRIM(mg.strand) = '-' THEN mg.gene_end ELSE mg.gene_start END",
                        direction,
                        "mg.id ASC");
                case "geneLog2fc" -> numericOrder("mg.avg_log2fc", direction, "mg.gene_symbol ASC, mg.id ASC");
                case "geneFdr" -> numericOrder("mg.fdr", direction, "mg.gene_symbol ASC, mg.id ASC");
                case "geneMeanDiff" -> numericOrder("mg.mean_diff", direction, "mg.gene_symbol ASC, mg.id ASC");
                default -> null;
            };
        }

        if (annotationType == AnnotationType.MARKER_PEAK) {
            return switch (key) {
                case "peakLog2fc" -> numericOrder("mp.log2fc", direction, "mp.chromosome ASC, mp.peak_start ASC, mp.id ASC");
                case "peakFdr" -> numericOrder("mp.fdr", direction, "mp.chromosome ASC, mp.peak_start ASC, mp.id ASC");
                case "peakMeanDiff" -> numericOrder("mp.mean_diff", direction, "mp.chromosome ASC, mp.peak_start ASC, mp.id ASC");
                default -> null;
            };
        }

        if (annotationType == AnnotationType.LINKED_REGION) {
            return switch (key) {
                case "linkScore" -> p2gFallback
                        ? numericOrder("COALESCE(p.link_score, ABS(p.correlation), 0)", direction, "p.id ASC")
                        : numericOrder("mlr.link_score", direction, "mlr.peak_start ASC, mlr.id ASC");
                case "geneEvidence" -> p2gFallback
                        ? textOrder("p.gene_name", direction, "p.id ASC")
                        : numericOrder("mlr.gene_log2fc", direction, "mlr.gene_symbol ASC, mlr.id ASC");
                case "peakEvidence" -> p2gFallback
                        ? numericOrder("COALESCE(p.link_score, ABS(p.correlation), 0)", direction, "p.id ASC")
                        : numericOrder("mlr.peak_log2fc", direction, "mlr.peak_start ASC, mlr.id ASC");
                default -> null;
            };
        }

        return null;
    }

    private String numericOrder(String expression, String direction, String tieBreakers) {
        return "(" + expression + " IS NULL) ASC, " + expression + " " + direction + ", " + tieBreakers;
    }

    private String textOrder(String expression, String direction, String tieBreakers) {
        return "(" + expression + " IS NULL OR " + expression + " = '') ASC, "
                + expression + " " + direction + ", " + tieBreakers;
    }

    private String genomicOrder(
            String chromosomeExpression,
            String startExpression,
            String endExpression,
            String direction,
            String tieBreakers
    ) {
        return "(" + chromosomeExpression + " IS NULL OR " + chromosomeExpression + " = '') ASC, "
                + chromosomeRankExpression(chromosomeExpression) + " " + direction + ", "
                + chromosomeExpression + " " + direction + ", "
                + startExpression + " " + direction + ", "
                + endExpression + " " + direction + ", "
                + tieBreakers;
    }

    private String chromosomeRankExpression(String chromosomeExpression) {
        StringBuilder expression = new StringBuilder("CASE LOWER(TRIM(")
                .append(chromosomeExpression)
                .append(")) ");
        for (int chromosome = 1; chromosome <= 22; chromosome++) {
            expression.append("WHEN 'chr").append(chromosome).append("' THEN ").append(chromosome).append(' ');
        }
        return expression
                .append("WHEN 'chrx' THEN 23 WHEN 'chry' THEN 24 ")
                .append("WHEN 'chrm' THEN 25 WHEN 'chrmt' THEN 25 ELSE 100 END")
                .toString();
    }

    private AnnotationType normalizeAnnotationType(String annotationType, String domain) {
        String normalized = trimToNull(annotationType);
        if (normalized == null) {
            if ("rna".equals(domain)) {
                return AnnotationType.MARKER_GENE;
            }
            if ("atac".equals(domain)) {
                return AnnotationType.MARKER_PEAK;
            }
            return AnnotationType.LINKED_REGION;
        }

        String key = normalized.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "gene", "marker_gene", "marker_genes" -> AnnotationType.MARKER_GENE;
            case "peak", "marker_peak", "marker_peaks" -> AnnotationType.MARKER_PEAK;
            case "p2g", "p2g_link", "p2g_links", "linked_region", "linked_regions", "linked_peak", "linked_peaks", "enhancer" -> AnnotationType.LINKED_REGION;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "annotationType is not supported");
        };
    }

    private RegionFilter normalizeRegionType(String regionType) {
        String normalized = trimToNull(regionType);
        if (normalized == null || "all".equalsIgnoreCase(normalized)) {
            return RegionFilter.ALL;
        }

        String key = normalized.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "promoter" -> RegionFilter.PROMOTER;
            case "enhancer", "candidate_enhancer", "te", "typical_enhancer" -> RegionFilter.ENHANCER;
            case "linked_peak", "linked_peaks" -> RegionFilter.LINKED_PEAK;
            case "super_enhancer", "superenhancer", "se" -> RegionFilter.SUPER_ENHANCER;
            default -> RegionFilter.UNSUPPORTED;
        };
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, HARD_PAGE_SIZE);
    }

    private Double normalizeMinLog2fc(Double minLog2fc) {
        if (minLog2fc == null || !Double.isFinite(minLog2fc)) {
            return null;
        }
        return Math.max(0.0d, minLog2fc);
    }

    private Double normalizeMinimumScore(Double minScore) {
        if (minScore == null || !Double.isFinite(minScore)) {
            return null;
        }
        return Math.max(0.0d, minScore);
    }

    private Double normalizeOptionalDouble(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        return value;
    }

    private int pageOffset(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    private int intOffset(long offset) {
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    private RegulatoryAnnotationResponse emptyResponse(int page, int pageSize) {
        return new RegulatoryAnnotationResponse(0L, page, pageSize, List.of());
    }

    private String exactGeneSymbol(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private PeakSearchFilter parsePeakSearchFilter(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return PeakSearchFilter.empty();
        }

        Matcher matcher = PEAK_REGION_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return PeakSearchFilter.exact(normalized);
        }

        try {
            String chromosome = normalizeChromosome(matcher.group(1));
            long start = Long.parseLong(matcher.group(2).replace(",", ""));
            long end = Long.parseLong(matcher.group(3).replace(",", ""));
            if (start >= end) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Region start must be smaller than region end."
                );
            }
            return PeakSearchFilter.overlap(chromosome, start, end);
        } catch (NumberFormatException invalidCoordinate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Region coordinates are outside the supported integer range."
            );
        }
    }

    private boolean isGenomicRegionSearch(String value) {
        String normalized = trimToNull(value);
        return normalized != null && normalized.replace(",", "").matches("(?i)^chr[^:]+:\\d+(-\\d+)?$");
    }

    private String normalizeChromosome(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() < 3 || !normalized.regionMatches(true, 0, "chr", 0, 3)) {
            return normalized;
        }
        return "chr" + normalized.substring(3);
    }

    private boolean sameSearchTerm(String first, String second) {
        String normalizedFirst = trimToNull(first);
        String normalizedSecond = trimToNull(second);
        return normalizedFirst != null && normalizedSecond != null && normalizedFirst.equalsIgnoreCase(normalizedSecond);
    }

    private void logQuery(
            AnnotationType annotationType,
            String datasetId,
            String requestedDomain,
            String dataDomain,
            int page,
            int pageSize,
            long total,
            long countMillis,
            long pageQueryMillis,
            long hydrateMillis,
            long overallMillis,
            String source,
            boolean countCacheHit
    ) {
        LOGGER.info(
                "Regulatory annotation query annotationType={} datasetId={} requestedDomain={} dataDomain={} page={} pageSize={} total={} countMillis={} pageQueryMillis={} hydrateMillis={} overallMillis={} source={} countCacheHit={}",
                annotationType.value(),
                datasetId,
                requestedDomain,
                dataDomain,
                page,
                pageSize,
                total,
                countMillis,
                pageQueryMillis,
                hydrateMillis,
                overallMillis,
                source,
                countCacheHit
        );
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String trimToEmpty(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private long subtractToZero(long value, long delta) {
        if (value <= delta) {
            return 0L;
        }
        return value - delta;
    }

    private long addSafely(long value, long delta) {
        if (value > Long.MAX_VALUE - delta) {
            return Long.MAX_VALUE;
        }
        return value + delta;
    }

    private String regionString(String chromosome, Long start, Long end) {
        String normalizedChromosome = trimToNull(chromosome);
        if (normalizedChromosome == null || start == null || end == null) {
            return "";
        }
        long s = start, e = end;
        if (s > e) { long t = s; s = e; e = t; }
        return normalizedChromosome + ":" + s + "-" + e;
    }

    private static Long normStart(Long start, Long end) {
        if (start == null || end == null) return start;
        return start > end ? end : start;
    }

    private static Long normEnd(Long start, Long end) {
        if (start == null || end == null) return end;
        return start > end ? start : end;
    }

    private String peakFeatureId(String chromosome, Long start, Long end) {
        String region = regionString(chromosome, start, end);
        return region.isEmpty() ? null : region;
    }

    private RegulatoryTfSummaryResponse tfUnavailable(
            String datasetId,
            String domain,
            String featureType,
            String featureId,
            String reason
    ) {
        return new RegulatoryTfSummaryResponse(
                datasetId,
                domain,
                featureType,
                featureId,
                false,
                "NOT_AVAILABLE",
                null,
                reason
        );
    }

    private enum AnnotationType {
        MARKER_GENE("marker_gene"),
        MARKER_PEAK("marker_peak"),
        LINKED_REGION("linked_region");

        private final String value;

        AnnotationType(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    private enum RegionFilter {
        ALL,
        PROMOTER,
        ENHANCER,
        LINKED_PEAK,
        SUPER_ENHANCER,
        UNSUPPORTED
    }

    private record FilterPatterns(
            String targetGene,
            String peakQuery,
            String contextCellType,
            String contextCluster
    ) {
    }

    private record CountFilters(
            String targetGene,
            String peak,
            String contextCellType,
            String contextCluster,
            String regionType,
            Double maxFdr,
            Double minLog2fc,
            Double minP2gScore
    ) {
    }

    private record PeakSearchFilter(
            String exactPeak,
            String chromosome,
            Long start,
            Long end,
            String cacheKey
    ) {
        private static PeakSearchFilter empty() {
            return new PeakSearchFilter(null, null, null, null, null);
        }

        private static PeakSearchFilter exact(String peak) {
            return new PeakSearchFilter(
                    peak,
                    null,
                    null,
                    null,
                    "exact:" + peak.toLowerCase(Locale.ROOT)
            );
        }

        private static PeakSearchFilter overlap(String chromosome, long start, long end) {
            return new PeakSearchFilter(
                    null,
                    chromosome,
                    start,
                    end,
                    "overlap:" + chromosome.toLowerCase(Locale.ROOT) + ":" + start + "-" + end
            );
        }
    }

    private record PromoterRegion(String chromosome, Long start, Long end) {
    }

    private record PagedRows(
            String dataDomain,
            long total,
            List<RegulatoryAnnotationRow> rows,
            long countMillis,
            long pageQueryMillis,
            long hydrateMillis,
            String source,
            boolean countCacheHit
    ) {
    }
}
