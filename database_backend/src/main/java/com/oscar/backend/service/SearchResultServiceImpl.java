package com.oscar.backend.service;

import com.oscar.backend.entity.SearchResultCellTypeCompositionResponse;
import com.oscar.backend.entity.SearchResultCellTypeItemResponse;
import com.oscar.backend.entity.SearchResultOverviewResponse;
import com.oscar.backend.entity.SearchResultQcGroupResponse;
import com.oscar.backend.entity.SearchResultQcMetricResponse;
import com.oscar.backend.entity.SearchResultQcSummaryRow;
import com.oscar.backend.entity.SearchResultQcValueRow;
import com.oscar.backend.entity.SearchResultQcViolinResponse;
import com.oscar.backend.entity.SearchResultUmapPointResponse;
import com.oscar.backend.entity.SearchResultUmapResponse;
import com.oscar.backend.mapper.SearchResultMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchResultServiceImpl implements SearchResultService {

    private static final int DEFAULT_UMAP_MAX_POINTS = 4000;
    private static final int MIN_UMAP_MAX_POINTS = 500;
    private static final int HARD_UMAP_MAX_POINTS = 5000;
    private static final int DEFAULT_QC_VALUES_PER_GROUP = 400;
    private static final int HARD_QC_VALUES_PER_GROUP = 500;
    private static final int UMAP_EXPORT_PAGE_SIZE = 5000;

    private static final String DEFAULT_EMBEDDING = "umap";

    private static final Set<String> SUPPORTED_DOMAINS = Set.of("integration", "rna", "atac");
    private static final Set<String> SUPPORTED_EMBEDDINGS = Set.of("umap", "tsne");

    private static final List<String> DEFAULT_QC_METRICS = List.of(
            "TSSEnrichment", "nFrags", "Gex_nGenes", "Gex_MitoRatio");

    private static final Map<String, MetricDefinition> METRIC_COLUMNS = createMetricColumns();

    private final SearchResultMapper searchResultMapper;

    public SearchResultServiceImpl(SearchResultMapper searchResultMapper) {
        this.searchResultMapper = searchResultMapper;
    }

    // ===================================================================
    // Public API
    // ===================================================================

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "sampleOverview", key = "#datasetId")
    public SearchResultOverviewResponse getOverview(String datasetId) {
        String id = normalizeRequiredDatasetId(datasetId);
        SearchResultOverviewResponse overview = searchResultMapper.selectOverviewByDatasetId(id);
        if (overview == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "datasetId not found");
        return overview;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "cellTypeComposition", key = "#datasetId + ':' + #domain")
    public SearchResultCellTypeCompositionResponse getCellTypeComposition(
            String datasetId, String domain, String groupBy) {
        String normalizedId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeDomain(domain);
        ensureVisibleSampleExists(normalizedId);
        ChartDomainContext ctx = resolveChartDomainContext(normalizedDomain, DEFAULT_EMBEDDING);

        List<SearchResultCellTypeItemResponse> items = searchResultMapper.selectCellTypeComposition(
                normalizedId, normalizedDomain, ctx.clusterRaw(), ctx.clusterExpr());
        return new SearchResultCellTypeCompositionResponse(normalizedId, "cluster", items);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "qcViolin", key = "#datasetId + ':' + #domain + ':' + #groupBy + ':' + #metrics")
    public SearchResultQcViolinResponse getQcViolin(
            String datasetId, String domain, String groupBy, String metrics) {
        String normalizedId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeDomain(domain);
        ensureVisibleSampleExists(normalizedId);
        ChartDomainContext ctx = resolveChartDomainContext(normalizedDomain, DEFAULT_EMBEDDING);

        List<MetricDefinition> requestedMetrics = resolveMetrics(metrics);
        List<SearchResultQcMetricResponse> metricResponses = new ArrayList<>();
        for (MetricDefinition metricDef : requestedMetrics) {
            metricResponses.add(buildQcMetricResponse(
                    normalizedId, normalizedDomain, ctx.clusterRaw(), ctx.clusterExpr(), metricDef));
        }
        return new SearchResultQcViolinResponse(normalizedId, "cluster", metricResponses);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "umapData", key = "#datasetId + ':' + #domain + ':' + #embedding + ':' + #colorBy + ':' + #maxPoints")
    public SearchResultUmapResponse getUmap(
            String datasetId, String domain, String embedding, String colorBy, Integer maxPoints) {
        String normalizedId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeDomain(domain);
        String normalizedEmbedding = normalizeEmbedding(embedding);
        String normalizedColorBy = normalizeColorBy(normalizedDomain, colorBy);
        int normalizedMaxPoints = normalizeMaxPoints(maxPoints);
        ensureVisibleSampleExists(normalizedId);
        ChartDomainContext ctx = resolveChartDomainContext(normalizedDomain, normalizedEmbedding);

        long total = searchResultMapper.countScatterPoints(normalizedId, ctx.xColumn(), ctx.yColumn());
        if (total == 0) {
            return new SearchResultUmapResponse(normalizedId, normalizedDomain, normalizedEmbedding,
                    normalizedColorBy, 0L, 0, List.of());
        }
        int samplingModulo = total > normalizedMaxPoints
                ? Math.max(1, (int) (total / normalizedMaxPoints)) : 1;

        List<SearchResultUmapPointResponse> points;
        if ("celltype".equals(normalizedColorBy)) {
            // integration only — color by global cell_type, no annotation JOIN
            points = searchResultMapper.selectSampledScatterPointsByCellType(
                    normalizedId, ctx.xColumn(), ctx.yColumn(),
                    ctx.clusterExpr(), samplingModulo, normalizedMaxPoints);
        } else {
            points = searchResultMapper.selectSampledScatterPoints(
                    normalizedId, normalizedDomain,
                    ctx.xColumn(), ctx.yColumn(),
                    ctx.clusterExpr(), ctx.clusterRaw(),
                    samplingModulo, normalizedMaxPoints);
        }
        return new SearchResultUmapResponse(normalizedId, normalizedDomain, normalizedEmbedding,
                normalizedColorBy, total, points.size(), points);
    }

    @Override
    public void writeFullUmapCsv(
            String datasetId,
            String domain,
            String embedding,
            String colorBy,
            OutputStream outputStream
    ) throws IOException {
        String normalizedId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeDomain(domain);
        String normalizedEmbedding = normalizeEmbedding(embedding);
        String normalizedColorBy = normalizeColorBy(normalizedDomain, colorBy);
        ensureVisibleSampleExists(normalizedId);
        ChartDomainContext ctx = resolveChartDomainContext(normalizedDomain, normalizedEmbedding);
        boolean includeCellType = "integration".equals(normalizedDomain);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            if (includeCellType) {
                writeCsvRow(writer, "dataset_id", "domain", "embedding", "color_by", "barcode",
                        "x", "y", "label", "cell_type", "cluster");
            } else {
                writeCsvRow(writer, "dataset_id", "domain", "embedding", "color_by", "barcode",
                        "x", "y", "label", "cluster");
            }

            String afterBarcode = null;
            while (true) {
                List<SearchResultUmapPointResponse> points;
                if ("celltype".equals(normalizedColorBy)) {
                    points = searchResultMapper.selectScatterPointsPageByCellType(
                            normalizedId,
                            ctx.xColumn(),
                            ctx.yColumn(),
                            ctx.clusterExpr(),
                            afterBarcode,
                            UMAP_EXPORT_PAGE_SIZE);
                } else {
                    points = searchResultMapper.selectScatterPointsPage(
                            normalizedId,
                            normalizedDomain,
                            ctx.xColumn(),
                            ctx.yColumn(),
                            ctx.clusterExpr(),
                            ctx.clusterRaw(),
                            afterBarcode,
                            UMAP_EXPORT_PAGE_SIZE);
                }

                for (SearchResultUmapPointResponse point : points) {
                    if (includeCellType) {
                        writeCsvRow(writer,
                                normalizedId,
                                normalizedDomain,
                                normalizedEmbedding,
                                normalizedColorBy,
                                point.getBarcode(),
                                point.getX(),
                                point.getY(),
                                point.getLabel(),
                                point.getCelltype(),
                                point.getCluster());
                    } else {
                        writeCsvRow(writer,
                                normalizedId,
                                normalizedDomain,
                                normalizedEmbedding,
                                normalizedColorBy,
                                point.getBarcode(),
                                point.getX(),
                                point.getY(),
                                point.getLabel(),
                                point.getCluster());
                    }
                }

                if (points.size() < UMAP_EXPORT_PAGE_SIZE) break;
                afterBarcode = points.get(points.size() - 1).getBarcode();
                writer.flush();
            }
        }
    }

    private void writeCsvRow(Writer writer, Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) writer.write(',');
            writer.write('"');
            String value = values[i] == null ? "" : values[i].toString();
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write('\n');
    }

    // ===================================================================
    // Domain + embedding → column mapping
    // ===================================================================

    /* ChartDomainContext holds four column expressions:
     *   xColumn / yColumn  — embedding columns for scatter
     *   clusterRaw         — bare column reference for JOIN with oscar_cluster_annotation
     *   clusterExpr        — COALESCE expression for SELECT / GROUP BY display value
     */

    private ChartDomainContext resolveChartDomainContext(String domain, String embedding) {
        return switch (domain) {
            case "integration" -> switch (embedding) {
                case "umap" -> new ChartDomainContext(
                        "COALESCE(c.umap_wnn_1, c.umap_1)",
                        "COALESCE(c.umap_wnn_2, c.umap_2)",
                        "c.cluster_wnn",
                        "COALESCE(NULLIF(TRIM(c.cluster_wnn), ''), COALESCE(NULLIF(TRIM(c.cluster_label), ''), 'Unknown'))");
                case "tsne" -> new ChartDomainContext(
                        "COALESCE(c.tsne_wnn_1, c.tsne_1)",
                        "COALESCE(c.tsne_wnn_2, c.tsne_2)",
                        "c.cluster_wnn",
                        "COALESCE(NULLIF(TRIM(c.cluster_wnn), ''), COALESCE(NULLIF(TRIM(c.cluster_label), ''), 'Unknown'))");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported embedding: " + embedding);
            };
            case "rna" -> switch (embedding) {
                case "umap" -> new ChartDomainContext(
                        "c.umap_rna_1", "c.umap_rna_2",
                        "c.cluster_rna",
                        "COALESCE(NULLIF(TRIM(c.cluster_rna), ''), 'Unknown')");
                case "tsne" -> new ChartDomainContext(
                        "c.tsne_rna_1", "c.tsne_rna_2",
                        "c.cluster_rna",
                        "COALESCE(NULLIF(TRIM(c.cluster_rna), ''), 'Unknown')");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported embedding: " + embedding);
            };
            case "atac" -> switch (embedding) {
                case "umap" -> new ChartDomainContext(
                        "c.umap_atac_1", "c.umap_atac_2",
                        "c.cluster_atac",
                        "COALESCE(NULLIF(TRIM(c.cluster_atac), ''), 'Unknown')");
                case "tsne" -> new ChartDomainContext(
                        "c.tsne_atac_1", "c.tsne_atac_2",
                        "c.cluster_atac",
                        "COALESCE(NULLIF(TRIM(c.cluster_atac), ''), 'Unknown')");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported embedding: " + embedding);
            };
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported chart domain: " + domain);
        };
    }

    // ===================================================================
    // QC helpers
    // ===================================================================

    private SearchResultQcMetricResponse buildQcMetricResponse(
            String datasetId, String domain, String clusterRaw, String clusterExpr,
            MetricDefinition metricDef) {
        List<SearchResultQcSummaryRow> summaries = searchResultMapper.selectQcSummaryByMetric(
                datasetId, domain, clusterRaw, clusterExpr, metricDef.columnName());
        // fast modulo sampling instead of ROW_NUMBER() window function
        long totalCells = summaries.stream().mapToLong(
                s -> s.getCount() == null ? 0 : s.getCount()).sum();
        int targetPerGroup = Math.min(DEFAULT_QC_VALUES_PER_GROUP, HARD_QC_VALUES_PER_GROUP);
        int avgGroupSize = summaries.isEmpty() ? 0 : (int) (totalCells / summaries.size());
        int samplingModulo = avgGroupSize > targetPerGroup
                ? Math.max(1, avgGroupSize / targetPerGroup) : 1;
        List<SearchResultQcValueRow> sampledValues = searchResultMapper.selectQcSampledValuesByModulo(
                datasetId, domain, clusterRaw, clusterExpr, metricDef.columnName(), samplingModulo);
        Map<String, List<Double>> valuesByGroup = sampledValues.stream()
                .filter(vr -> vr.getValue() != null)
                .collect(Collectors.groupingBy(SearchResultQcValueRow::getLabel,
                        LinkedHashMap::new,
                        Collectors.mapping(SearchResultQcValueRow::getValue, Collectors.toList())));

        List<SearchResultQcGroupResponse> groups = summaries.stream()
                .map(s -> buildQcGroup(s, valuesByGroup.getOrDefault(s.getLabel(), List.of())))
                .filter(g -> !g.getValues().isEmpty())
                .toList();
        return new SearchResultQcMetricResponse(metricDef.requestName(), metricDef.label(), groups);
    }

    private SearchResultQcGroupResponse buildQcGroup(SearchResultQcSummaryRow summary, List<Double> sampled) {
        List<Double> sorted = sampled.stream().filter(Objects::nonNull).filter(Double::isFinite)
                .sorted(Comparator.naturalOrder()).toList();
        SearchResultQcGroupResponse g = new SearchResultQcGroupResponse();
        g.setLabel(summary.getLabel());
        g.setCount(summary.getCount());
        g.setMin(summary.getMin());
        g.setMax(summary.getMax());
        g.setValues(sorted);
        if (!sorted.isEmpty()) {
            g.setQ1(quantile(sorted, 0.25));
            g.setMedian(quantile(sorted, 0.5));
            g.setQ3(quantile(sorted, 0.75));
        }
        return g;
    }

    private Double quantile(List<Double> sorted, double q) {
        if (sorted.isEmpty()) return null;
        double pos = (sorted.size() - 1) * q;
        int base = (int) Math.floor(pos);
        double rem = pos - base;
        if (base + 1 >= sorted.size()) return sorted.get(base);
        return sorted.get(base) + rem * (sorted.get(base + 1) - sorted.get(base));
    }

    // ===================================================================
    // Normalisation / validation
    // ===================================================================

    private String normalizeRequiredDatasetId(String datasetId) {
        String v = trimToNull(datasetId);
        if (v == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datasetId is required");
        return v;
    }

    private void ensureVisibleSampleExists(String datasetId) {
        if (searchResultMapper.countVisibleSampleByDatasetId(datasetId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "datasetId not found");
    }

    private String normalizeDomain(String domain) {
        String v = trimToNull(domain);
        String d = v == null ? "integration" : v.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_DOMAINS.contains(d))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain must be integration, rna, or atac");
        return d;
    }

    private String normalizeColorBy(String domain, String colorBy) {
        String v = trimToNull(colorBy);
        String key = v == null
                ? ("integration".equals(domain) ? "celltype" : "cluster")
                : v.toLowerCase(Locale.ROOT);
        if (!Set.of("celltype", "cluster").contains(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "colorBy must be celltype or cluster");
        }
        // RNA / ATAC only support cluster-based coloring
        if ("celltype".equals(key) && !"integration".equals(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "colorBy=celltype is only available for domain=integration. " +
                    "Use colorBy=cluster for rna/atac.");
        }
        return key;
    }

    private String normalizeEmbedding(String embedding) {
        String v = trimToNull(embedding);
        String e = v == null ? DEFAULT_EMBEDDING : v.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EMBEDDINGS.contains(e))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "embedding must be umap or tsne");
        return e;
    }

    private int normalizeMaxPoints(Integer maxPoints) {
        if (maxPoints == null) return DEFAULT_UMAP_MAX_POINTS;
        if (maxPoints < MIN_UMAP_MAX_POINTS) return MIN_UMAP_MAX_POINTS;
        return Math.min(maxPoints, HARD_UMAP_MAX_POINTS);
    }

    private List<MetricDefinition> resolveMetrics(String metrics) {
        String v = trimToNull(metrics);
        List<String> names = v == null ? DEFAULT_QC_METRICS
                : Arrays.stream(v.split(",")).map(this::trimToNull).filter(Objects::nonNull).toList();
        return names.stream().map(METRIC_COLUMNS::get).filter(Objects::nonNull).distinct().toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static Map<String, MetricDefinition> createMetricColumns() {
        Map<String, MetricDefinition> m = new LinkedHashMap<>();
        m.put("TSSEnrichment", new MetricDefinition("TSSEnrichment", "TSSEnrichment", "c.tss_enrichment"));
        m.put("FRIP", new MetricDefinition("FRIP", "FRIP", "c.frip"));
        m.put("NucleosomeRatio", new MetricDefinition("NucleosomeRatio", "NucleosomeRatio", "c.nucleosome_ratio"));
        m.put("nFrags", new MetricDefinition("nFrags", "nFrags", "c.n_frags"));
        m.put("ReadsInPeaks", new MetricDefinition("ReadsInPeaks", "ReadsInPeaks", "c.reads_in_peaks"));
        m.put("BlacklistRatio", new MetricDefinition("BlacklistRatio", "BlacklistRatio", "c.blacklist_fraction"));
        m.put("Gex_nUMI", new MetricDefinition("Gex_nUMI", "Gex_nUMI", "c.gex_n_umi"));
        m.put("Gex_nGenes", new MetricDefinition("Gex_nGenes", "Gex_nGenes", "c.gex_n_genes"));
        m.put("Gex_MitoRatio", new MetricDefinition("Gex_MitoRatio", "Gex_MitoRatio", "c.gex_mito_ratio"));
        m.put("Gex_RiboRatio", new MetricDefinition("Gex_RiboRatio", "Gex_RiboRatio", "c.gex_ribo_ratio"));
        m.put("DoubletScore", new MetricDefinition("DoubletScore", "DoubletScore", "c.doublet_score"));
        m.put("PromoterRatio", new MetricDefinition("PromoterRatio", "PromoterRatio", "c.promoter_ratio"));
        return m;
    }

    // ===================================================================
    // Records
    // ===================================================================

    /**
     * @param xColumn     embedding X column expression
     * @param yColumn     embedding Y column expression
     * @param clusterRaw  bare column for JOIN ON  (e.g. c.cluster_wnn)
     * @param clusterExpr COALESCE expression for SELECT/GROUP BY display value
     */
    private record ChartDomainContext(String xColumn, String yColumn,
                                      String clusterRaw, String clusterExpr) {
    }

    private record MetricDefinition(String requestName, String label, String columnName) {
    }
}
