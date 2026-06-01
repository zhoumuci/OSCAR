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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SearchResultServiceImpl implements SearchResultService {

    private static final int DEFAULT_UMAP_MAX_POINTS = 4000;
    private static final int MIN_UMAP_MAX_POINTS = 500;
    private static final int HARD_UMAP_MAX_POINTS = 5000;
    private static final int DEFAULT_QC_VALUES_PER_GROUP = 400;
    private static final int HARD_QC_VALUES_PER_GROUP = 500;
    private static final String DEFAULT_SEARCH_RESULT_DOMAIN = "integration";
    private static final String DEFAULT_SCATTER_EMBEDDING = "umap";
    private static final List<String> SEARCH_RESULT_DOMAINS = List.of(
            DEFAULT_SEARCH_RESULT_DOMAIN,
            "rna",
            "atac"
    );
    private static final List<String> DEFAULT_QC_METRICS = List.of(
            "TSSEnrichment",
            "FRIP",
            "NucleosomeRatio",
            "nFrags"
    );

    private static final Map<String, String> GROUP_COLUMNS = Map.of(
            "celltype", "c.cell_type",
            "cluster", "c.cluster_label"
    );

    private static final Map<String, String> CELL_PROFILE_DOMAIN_COLUMNS = Map.of(
            "domain", "c.domain",
            "view_type", "c.view_type",
            "modality", "c.modality"
    );

    private static final Map<String, EmbeddingDefinition> SCATTER_EMBEDDINGS = Map.of(
            DEFAULT_SCATTER_EMBEDDING, new EmbeddingDefinition(DEFAULT_SCATTER_EMBEDDING, "c.umap_1", "c.umap_2"),
            "tsne", new EmbeddingDefinition("tsne", "c.tsne_1", "c.tsne_2")
    );

    private static final Map<String, MetricDefinition> METRIC_COLUMNS = createMetricColumns();

    private final SearchResultMapper searchResultMapper;
    private volatile boolean cellProfileDomainColumnResolved;
    private volatile String cellProfileDomainColumn;

    public SearchResultServiceImpl(SearchResultMapper searchResultMapper) {
        this.searchResultMapper = searchResultMapper;
    }

    @Override
    public SearchResultOverviewResponse getOverview(String datasetId) {
        String normalizedDatasetId = normalizeRequiredDatasetId(datasetId);
        SearchResultOverviewResponse overview = searchResultMapper.selectOverviewByDatasetId(normalizedDatasetId);

        if (overview == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "datasetId not found");
        }

        return overview;
    }

    @Override
    public SearchResultCellTypeCompositionResponse getCellTypeComposition(String datasetId, String domain, String groupBy) {
        String normalizedDatasetId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeSearchResultDomain(domain);
        String normalizedGroupBy = normalizeGroupKey(groupBy, "groupBy");
        ensureVisibleSampleExists(normalizedDatasetId);
        ChartDomainContext domainContext = resolveChartDomainContext(normalizedDomain);

        List<SearchResultCellTypeItemResponse> items = searchResultMapper.selectCellTypeComposition(
                normalizedDatasetId,
                domainContext.domain(),
                domainContext.domainColumn(),
                GROUP_COLUMNS.get(normalizedGroupBy)
        );

        return new SearchResultCellTypeCompositionResponse(normalizedDatasetId, normalizedGroupBy, items);
    }

    @Override
    public SearchResultQcViolinResponse getQcViolin(String datasetId, String domain, String groupBy, String metrics) {
        String normalizedDatasetId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeSearchResultDomain(domain);
        String normalizedGroupBy = normalizeGroupKey(groupBy, "groupBy");
        ensureVisibleSampleExists(normalizedDatasetId);
        ChartDomainContext domainContext = resolveChartDomainContext(normalizedDomain);

        List<MetricDefinition> requestedMetrics = resolveMetrics(metrics);
        List<SearchResultQcMetricResponse> metricResponses = new ArrayList<>();

        for (MetricDefinition metricDefinition : requestedMetrics) {
            metricResponses.add(buildQcMetricResponse(
                    normalizedDatasetId,
                    domainContext,
                    normalizedGroupBy,
                    GROUP_COLUMNS.get(normalizedGroupBy),
                    metricDefinition
            ));
        }

        return new SearchResultQcViolinResponse(normalizedDatasetId, normalizedGroupBy, metricResponses);
    }

    @Override
    public SearchResultUmapResponse getUmap(
            String datasetId,
            String domain,
            String embedding,
            String colorBy,
            Integer maxPoints
    ) {
        String normalizedDatasetId = normalizeRequiredDatasetId(datasetId);
        String normalizedDomain = normalizeSearchResultDomain(domain);
        EmbeddingDefinition embeddingDefinition = normalizeScatterEmbedding(embedding);
        String normalizedColorBy = normalizeGroupKey(colorBy, "colorBy");
        int normalizedMaxPoints = normalizeMaxPoints(maxPoints);
        ensureVisibleSampleExists(normalizedDatasetId);
        ChartDomainContext domainContext = resolveChartDomainContext(normalizedDomain);

        long total = searchResultMapper.countScatterPoints(
                normalizedDatasetId,
                domainContext.domain(),
                domainContext.domainColumn(),
                embeddingDefinition.xColumn(),
                embeddingDefinition.yColumn()
        );
        if (total == 0) {
            return new SearchResultUmapResponse(
                    normalizedDatasetId,
                    normalizedDomain,
                    embeddingDefinition.name(),
                    normalizedColorBy,
                    0L,
                    0,
                    List.of()
            );
        }

        /*
         * Scatter points are intentionally capped for performance. The modulo
         * filter gives a stable barcode-hash sample for large datasets, and LIMIT
         * is the hard stop.
         */
        int samplingModulo = total > normalizedMaxPoints
                ? Math.max(1, (int) (total / normalizedMaxPoints))
                : 1;
        List<SearchResultUmapPointResponse> points = searchResultMapper.selectSampledScatterPoints(
                normalizedDatasetId,
                domainContext.domain(),
                domainContext.domainColumn(),
                embeddingDefinition.xColumn(),
                embeddingDefinition.yColumn(),
                GROUP_COLUMNS.get(normalizedColorBy),
                samplingModulo,
                normalizedMaxPoints
        );

        return new SearchResultUmapResponse(
                normalizedDatasetId,
                normalizedDomain,
                embeddingDefinition.name(),
                normalizedColorBy,
                total,
                points.size(),
                points
        );
    }

    private SearchResultQcMetricResponse buildQcMetricResponse(
            String datasetId,
            ChartDomainContext domainContext,
            String groupBy,
            String groupColumn,
            MetricDefinition metricDefinition
    ) {
        List<SearchResultQcSummaryRow> summaries = searchResultMapper.selectQcSummaryByMetric(
                datasetId,
                domainContext.domain(),
                domainContext.domainColumn(),
                groupColumn,
                metricDefinition.columnName()
        );
        List<SearchResultQcValueRow> sampledValues = searchResultMapper.selectQcSampledValuesByMetric(
                datasetId,
                domainContext.domain(),
                domainContext.domainColumn(),
                groupColumn,
                metricDefinition.columnName(),
                Math.min(DEFAULT_QC_VALUES_PER_GROUP, HARD_QC_VALUES_PER_GROUP)
        );
        Map<String, List<Double>> valuesByGroup = sampledValues.stream()
                .filter(valueRow -> valueRow.getValue() != null)
                .collect(Collectors.groupingBy(
                        SearchResultQcValueRow::getLabel,
                        LinkedHashMap::new,
                        Collectors.mapping(SearchResultQcValueRow::getValue, Collectors.toList())
                ));

        /*
         * Exact quartiles in MySQL are verbose and version-sensitive. The response
         * uses exact SQL count/min/max and sampled, capped values for q1/median/q3
         * so the violin/boxplot stays stable without returning every cell.
         */
        List<SearchResultQcGroupResponse> groups = summaries.stream()
                .map(summary -> buildQcGroup(summary, valuesByGroup.getOrDefault(summary.getLabel(), List.of())))
                .filter(group -> !group.getValues().isEmpty())
                .toList();

        return new SearchResultQcMetricResponse(metricDefinition.requestName(), metricDefinition.label(), groups);
    }

    private SearchResultQcGroupResponse buildQcGroup(SearchResultQcSummaryRow summary, List<Double> sampledValues) {
        List<Double> sortedValues = sampledValues.stream()
                .filter(Objects::nonNull)
                .filter(Double::isFinite)
                .sorted(Comparator.naturalOrder())
                .toList();

        SearchResultQcGroupResponse group = new SearchResultQcGroupResponse();
        group.setLabel(summary.getLabel());
        group.setCount(summary.getCount());
        group.setMin(summary.getMin());
        group.setMax(summary.getMax());
        group.setValues(sortedValues);

        if (!sortedValues.isEmpty()) {
            group.setQ1(quantile(sortedValues, 0.25));
            group.setMedian(quantile(sortedValues, 0.5));
            group.setQ3(quantile(sortedValues, 0.75));
        }

        return group;
    }

    private Double quantile(List<Double> sortedValues, double quantile) {
        if (sortedValues.isEmpty()) {
            return null;
        }

        double position = (sortedValues.size() - 1) * quantile;
        int baseIndex = (int) Math.floor(position);
        double remainder = position - baseIndex;

        if (baseIndex + 1 >= sortedValues.size()) {
            return sortedValues.get(baseIndex);
        }

        double currentValue = sortedValues.get(baseIndex);
        double nextValue = sortedValues.get(baseIndex + 1);
        return currentValue + remainder * (nextValue - currentValue);
    }

    private String normalizeRequiredDatasetId(String datasetId) {
        String normalizedDatasetId = trimToNull(datasetId);
        if (normalizedDatasetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datasetId is required");
        }

        return normalizedDatasetId;
    }

    private void ensureVisibleSampleExists(String datasetId) {
        if (searchResultMapper.countVisibleSampleByDatasetId(datasetId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "datasetId not found");
        }
    }

    private String normalizeSearchResultDomain(String requestedDomain) {
        String normalizedDomain = trimToNull(requestedDomain);
        String domain = normalizedDomain == null
                ? DEFAULT_SEARCH_RESULT_DOMAIN
                : normalizedDomain.toLowerCase(Locale.ROOT);

        if (!SEARCH_RESULT_DOMAINS.contains(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain must be integration, rna, or atac");
        }

        return domain;
    }

    private ChartDomainContext resolveChartDomainContext(String domain) {
        String domainColumn = resolveCellProfileDomainColumn();
        if (domainColumn == null && !DEFAULT_SEARCH_RESULT_DOMAIN.equals(domain)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "SearchResult chart data is not available for domain: " + domain
            );
        }

        return new ChartDomainContext(domain, domainColumn);
    }

    private String resolveCellProfileDomainColumn() {
        if (!cellProfileDomainColumnResolved) {
            synchronized (this) {
                if (!cellProfileDomainColumnResolved) {
                    cellProfileDomainColumn = normalizeCellProfileDomainColumn(
                            searchResultMapper.selectCellProfileDomainColumn()
                    );
                    cellProfileDomainColumnResolved = true;
                }
            }
        }

        return cellProfileDomainColumn;
    }

    private String normalizeCellProfileDomainColumn(String discoveredColumn) {
        String normalizedColumn = trimToNull(discoveredColumn);
        if (normalizedColumn == null) {
            return null;
        }

        return CELL_PROFILE_DOMAIN_COLUMNS.get(normalizedColumn.toLowerCase(Locale.ROOT));
    }

    private EmbeddingDefinition normalizeScatterEmbedding(String requestedEmbedding) {
        String normalizedEmbedding = trimToNull(requestedEmbedding);
        String embedding = normalizedEmbedding == null
                ? DEFAULT_SCATTER_EMBEDDING
                : normalizedEmbedding.toLowerCase(Locale.ROOT);

        EmbeddingDefinition embeddingDefinition = SCATTER_EMBEDDINGS.get(embedding);
        if (embeddingDefinition == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "embedding must be umap or tsne");
        }

        return embeddingDefinition;
    }

    private String normalizeGroupKey(String requestedValue, String parameterName) {
        String normalizedValue = trimToNull(requestedValue);
        String groupKey = normalizedValue == null ? "celltype" : normalizedValue.toLowerCase(Locale.ROOT);

        if (!GROUP_COLUMNS.containsKey(groupKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " must be celltype or cluster");
        }

        return groupKey;
    }

    private int normalizeMaxPoints(Integer maxPoints) {
        if (maxPoints == null) {
            return DEFAULT_UMAP_MAX_POINTS;
        }

        if (maxPoints < MIN_UMAP_MAX_POINTS) {
            return MIN_UMAP_MAX_POINTS;
        }

        return Math.min(maxPoints, HARD_UMAP_MAX_POINTS);
    }

    private List<MetricDefinition> resolveMetrics(String metrics) {
        String normalizedMetrics = trimToNull(metrics);
        List<String> requestedMetricNames = normalizedMetrics == null
                ? DEFAULT_QC_METRICS
                : Arrays.stream(normalizedMetrics.split(","))
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();

        /*
         * Metric names are ignored unless they match this whitelist. This prevents
         * request-controlled column names from reaching SQL.
         */
        return requestedMetricNames.stream()
                .map(METRIC_COLUMNS::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private static Map<String, MetricDefinition> createMetricColumns() {
        Map<String, MetricDefinition> metricColumns = new LinkedHashMap<>();
        metricColumns.put("TSSEnrichment", new MetricDefinition("TSSEnrichment", "TSSEnrichment", "c.tss_enrichment"));
        metricColumns.put("FRIP", new MetricDefinition("FRIP", "FRIP", "c.frip"));
        metricColumns.put("NucleosomeRatio", new MetricDefinition("NucleosomeRatio", "NucleosomeRatio", "c.nucleosome_ratio"));
        metricColumns.put("nFrags", new MetricDefinition("nFrags", "nFrags", "c.n_frags"));
        metricColumns.put("ReadsInPeaks", new MetricDefinition("ReadsInPeaks", "ReadsInPeaks", "c.reads_in_peaks"));
        metricColumns.put("BlacklistRatio", new MetricDefinition("BlacklistRatio", "BlacklistRatio", "c.blacklist_fraction"));
        metricColumns.put("Gex_nUMI", new MetricDefinition("Gex_nUMI", "Gex_nUMI", "c.gex_n_umi"));
        metricColumns.put("Gex_nGenes", new MetricDefinition("Gex_nGenes", "Gex_nGenes", "c.gex_n_genes"));
        metricColumns.put("Gex_MitoRatio", new MetricDefinition("Gex_MitoRatio", "Gex_MitoRatio", "c.gex_mito_ratio"));
        metricColumns.put("Gex_RiboRatio", new MetricDefinition("Gex_RiboRatio", "Gex_RiboRatio", "c.gex_ribo_ratio"));
        metricColumns.put("DoubletScore", new MetricDefinition("DoubletScore", "DoubletScore", "c.doublet_score"));
        metricColumns.put("PromoterRatio", new MetricDefinition("PromoterRatio", "PromoterRatio", "c.promoter_ratio"));
        return metricColumns;
    }

    private record MetricDefinition(String requestName, String label, String columnName) {
    }

    private record ChartDomainContext(String domain, String domainColumn) {
    }

    private record EmbeddingDefinition(String name, String xColumn, String yColumn) {
    }
}
