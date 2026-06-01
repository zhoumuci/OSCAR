package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryAnnotationRecord;
import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.entity.RegulatoryAnnotationRow;
import com.oscar.backend.entity.RegulatoryTfSummaryResponse;
import com.oscar.backend.mapper.RegulatoryAnnotationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RegulatoryAnnotationServiceImpl implements RegulatoryAnnotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatoryAnnotationServiceImpl.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int HARD_PAGE_SIZE = 100;

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
            Double minLog2fc
    ) {
        long overallStartedAt = System.nanoTime();
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        AnnotationType normalizedAnnotationType = normalizeAnnotationType(annotationType, normalizedDomain);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
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
        String countRegionType = normalizedAnnotationType == AnnotationType.LINKED_REGION
                ? "p2g_link_id"
                : effectiveRegionType.name().toLowerCase(Locale.ROOT);
        FilterPatterns filters = buildFilterPatterns(normalizedAnnotationType, targetGene, peak, contextCellType, contextCluster);
        CountFilters countFilters = new CountFilters(
                filters.targetGene(),
                filters.peakPattern(),
                filters.contextCellType(),
                filters.contextCluster(),
                countRegionType,
                normalizedMaxFdr,
                normalizedMinLog2fc
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
                    offset
            );
            case MARKER_PEAK -> selectMarkerPeakRows(
                    normalizedDatasetId,
                    normalizedDomain,
                    filters.targetGene(),
                    filters.peakPattern(),
                    filters.contextCellType(),
                    filters.contextCluster(),
                    normalizedMaxFdr,
                    normalizedMinLog2fc,
                    countFilters,
                    normalizedPageSize,
                    offset
            );
            case LINKED_REGION -> selectLinkedRegionRows(
                    normalizedDatasetId,
                    normalizedDomain,
                    filters.targetGene(),
                    filters.peakPattern(),
                    filters.contextCellType(),
                    filters.contextCluster(),
                    normalizedMaxFdr,
                    normalizedMinLog2fc,
                    countFilters,
                    normalizedPageSize,
                    offset
            );
        };

        long hydrateStartedAt = System.nanoTime();
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
        return normalizeContextOptions(options);
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
            long offset
    ) {
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.MARKER_GENE,
                datasetId,
                domain,
                countFilters,
                () -> regulatoryAnnotationMapper.countMarkerGenes(
                        datasetId,
                        domain,
                        targetGene,
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
                limit,
                intOffset(offset)
        );
        return new PagedRows(dataDomain, total, rows, countMillis, elapsedMillis(pageStartedAt), 0L, "marker_gene", countCacheHit);
    }

    private PagedRows selectMarkerPeakRows(
            String datasetId,
            String domain,
            String targetGene,
            String peakPattern,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            CountFilters countFilters,
            int limit,
            long offset
    ) {
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.MARKER_PEAK,
                datasetId,
                domain,
                countFilters,
                () -> regulatoryAnnotationMapper.countMarkerPeaks(
                        datasetId,
                        domain,
                        targetGene,
                        peakPattern,
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
                peakPattern,
                contextCellType,
                contextCluster,
                maxFdr,
                minLog2fc,
                limit,
                intOffset(offset)
        );
        if (pageIds == null || pageIds.isEmpty()) {
            return new PagedRows(dataDomain, total, List.of(), countMillis, elapsedMillis(pageStartedAt), 0L, "marker_peak", countCacheHit);
        }
        List<RegulatoryAnnotationRow> rows = regulatoryAnnotationMapper.selectMarkerPeaksByIds(pageIds);
        long pageQueryMillis = elapsedMillis(pageStartedAt);
        long hydrateStartedAt = System.nanoTime();
        mergeMarkerPeakLinkSummaries(datasetId, dataDomain, rows);
        long hydrateMillis = elapsedMillis(hydrateStartedAt);
        return new PagedRows(dataDomain, total, rows, countMillis, pageQueryMillis, hydrateMillis, "marker_peak", countCacheHit);
    }

    private PagedRows selectLinkedRegionRows(
            String datasetId,
            String domain,
            String targetGene,
            String peakPattern,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            CountFilters countFilters,
            int limit,
            long offset
    ) {
        String dataDomain = domain;
        RegulatoryAnnotationCountCache.CountResult countResult = cachedCount(
                AnnotationType.LINKED_REGION,
                datasetId,
                dataDomain,
                countFilters,
                () -> regulatoryAnnotationMapper.countLinkedRegions(
                        datasetId,
                        dataDomain,
                        targetGene,
                        peakPattern,
                        contextCellType,
                        contextCluster,
                        maxFdr,
                        minLog2fc
                )
        );
        long total = countResult.total();
        long countMillis = countResult.countMillis();
        boolean countCacheHit = countResult.cacheHit();
        if (total == 0L || offset >= total) {
            if (total == 0L && !hasLinkedRegionMaterializedRows(datasetId, dataDomain)) {
                LOGGER.warn(
                        "linked_region materialized table is empty; run refresh endpoint. datasetId={} requestedDomain={} dataDomain={}",
                        datasetId,
                        domain,
                        dataDomain
                );
            }
            return new PagedRows(dataDomain, total, List.of(), countMillis, 0L, 0L, "materialized_linked_region", countCacheHit);
        }
        long pageStartedAt = System.nanoTime();
        List<Long> pageIds = regulatoryAnnotationMapper.selectLinkedRegionPageIds(
                datasetId,
                dataDomain,
                targetGene,
                peakPattern,
                contextCellType,
                contextCluster,
                maxFdr,
                minLog2fc,
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

    private void mergeMarkerPeakLinkSummaries(String datasetId, String dataDomain, List<RegulatoryAnnotationRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        List<RegulatoryAnnotationRow> linkSummaries = regulatoryAnnotationMapper.selectMarkerPeakLinkSummaries(
                datasetId,
                dataDomain,
                rows
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

    private boolean hasLinkedRegionMaterializedRows(String datasetId, String dataDomain) {
        return regulatoryAnnotationMapper.existsLinkedRegionMaterializedRows(datasetId, dataDomain) == 1;
    }

    private RegulatoryAnnotationCountCache.CountResult cachedCount(
            AnnotationType annotationType,
            String datasetId,
            String dataDomain,
            CountFilters filters,
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
                        filters.minLog2fc()
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

        PromoterRegion promoter = calculatePromoterRegion(
                row.getGeneChromosome(),
                row.getGeneStart(),
                row.getGeneEnd(),
                row.getStrand()
        );
        String normalizedStrand = normalizeStrand(row.getStrand());
        String geneRegion = regionString(row.getGeneChromosome(), row.getGeneStart(), row.getGeneEnd());
        String promoterRegion = regionString(promoter.chromosome(), promoter.start(), promoter.end());

        RegulatoryAnnotationRecord record = baseRecord(row, AnnotationType.MARKER_GENE.value());
        record.setId("marker-gene-" + row.getMarkerGeneId());
        record.setTargetGene(gene);
        record.setGeneSymbol(gene);
        record.setGene(gene);
        record.setGeneId(trimToEmpty(row.getGeneId()));
        record.setGeneChromosome(trimToEmpty(row.getGeneChromosome()));
        record.setGeneStart(row.getGeneStart());
        record.setGeneEnd(row.getGeneEnd());
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
        String peakName = firstNonBlank(row.getPeakName(), peakRegion);
        String linkedGene = trimToEmpty(row.getLinkedGeneName());
        String outputRegionType = peakRegionType(row, requestedRegionType);

        RegulatoryAnnotationRecord record = baseRecord(row, AnnotationType.MARKER_PEAK.value());
        record.setId(markerPeakId(row));
        record.setTargetGene(linkedGene);
        record.setPeakName(peakName);
        record.setPeak(trimToEmpty(row.getPeakName()));
        record.setPeakId(peakName);
        record.setPeakChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setPeakStart(row.getPeakStart());
        record.setPeakEnd(row.getPeakEnd());
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
        record.setStart(row.getPeakStart());
        record.setEnd(row.getPeakEnd());
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

        String geneRegion = regionString(row.getGeneChromosome(), row.getGeneStart(), row.getGeneEnd());
        PromoterRegion promoter = calculatePromoterRegion(
                row.getGeneChromosome(),
                row.getGeneStart(),
                row.getGeneEnd(),
                row.getStrand()
        );
        String normalizedStrand = normalizeStrand(row.getStrand());
        String promoterRegion = regionString(promoter.chromosome(), promoter.start(), promoter.end());
        String peakRegion = firstNonBlank(
                row.getPeakRegion(),
                regionString(row.getPeakChromosome(), row.getPeakStart(), row.getPeakEnd())
        );
        String peakName = trimToEmpty(row.getPeakName());
        String linkedPeak = firstNonBlank(row.getPeakName(), peakRegion);
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
        record.setGeneStart(row.getGeneStart());
        record.setGeneEnd(row.getGeneEnd());
        record.setGeneRegion(geneRegion);
        record.setStrand(normalizedStrand);
        record.setPromoterRegion(promoterRegion);
        record.setGeneLog2fc(row.getAvgLog2fc());
        record.setGeneFdr(row.getGeneFdr());
        record.setGeneMeanDiff(row.getGeneMeanDiff());
        record.setPeakName(peakName);
        record.setPeak(trimToEmpty(row.getPeakName()));
        record.setPeakId(linkedPeak);
        record.setPeakChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setPeakStart(row.getPeakStart());
        record.setPeakEnd(row.getPeakEnd());
        record.setPeakRegion(peakRegion);
        record.setPeakLog2fc(row.getPeakLog2fc());
        record.setPeakFdr(row.getPeakFdr());
        record.setPeakMeanDiff(row.getPeakMeanDiff());
        record.setLinkedPeak(linkedPeak);
        record.setLinkedGene(firstNonBlank(row.getLinkedGeneName(), gene));
        record.setLinkScore(row.getLinkScore());
        record.setCorrelation(row.getCorrelation());
        record.setLinkFdr(row.getLinkFdr());
        record.setDistance(distanceToTss(row));
        record.setRegulatoryRegion(peakRegion);
        record.setChromosome(trimToEmpty(row.getPeakChromosome()));
        record.setStart(row.getPeakStart());
        record.setEnd(row.getPeakEnd());
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
        String cellType = trimToNull(row.getCellType());
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
        String peakPattern = likePattern(normalizedPeak);

        if (annotationType == AnnotationType.MARKER_PEAK && sameSearchTerm(targetGene, normalizedPeak)) {
            if (isGenomicRegionSearch(normalizedPeak)) {
                normalizedTargetGene = null;
            } else {
                peakPattern = null;
            }
        }

        return new FilterPatterns(
                normalizedTargetGene,
                peakPattern,
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

    private List<RegulatoryAnnotationContextOption> normalizeContextOptions(List<RegulatoryAnnotationContextOption> rawOptions) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            return List.of();
        }

        List<RegulatoryAnnotationContextOption> options = new ArrayList<>();
        for (RegulatoryAnnotationContextOption rawOption : rawOptions) {
            if (rawOption == null) {
                continue;
            }

            String cellType = trimToNull(rawOption.getCellType());
            String cluster = trimToNull(rawOption.getCluster());
            String label = contextOptionLabel(cellType, cluster);
            if (label == null) {
                continue;
            }

            RegulatoryAnnotationContextOption option = new RegulatoryAnnotationContextOption();
            option.setCellType(cellType);
            option.setCluster(cluster);
            option.setLabel(label);
            option.setValue(contextOptionValue(cellType, cluster));
            option.setCount(rawOption.getCount());
            options.add(option);
        }
        return options;
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

        long promoterStart;
        long promoterEnd;
        if (minusStrand) {
            promoterStart = subtractToZero(tss, 500L);
            promoterEnd = addSafely(tss, 2000L);
        } else {
            promoterStart = subtractToZero(tss, 2000L);
            promoterEnd = addSafely(tss, 500L);
        }
        return new PromoterRegion(normalizedChromosome, promoterStart, promoterEnd);
    }

    private Long distanceToTss(RegulatoryAnnotationRow row) {
        Long tss = "-".equals(normalizeStrand(row.getStrand())) ? row.getGeneEnd() : row.getGeneStart();
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

    private String likePattern(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : "%" + normalized + "%";
    }

    private String exactGeneSymbol(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private boolean isGenomicRegionSearch(String value) {
        String normalized = trimToNull(value);
        return normalized != null && normalized.replace(",", "").matches("(?i)^chr[^:]+:\\d+(-\\d+)?$");
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
        return normalizedChromosome + ":" + start + "-" + end;
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
            String peakPattern,
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
            Double minLog2fc
    ) {
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
