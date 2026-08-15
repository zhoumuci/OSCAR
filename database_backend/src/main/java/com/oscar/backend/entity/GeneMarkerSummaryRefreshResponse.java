package com.oscar.backend.entity;

import java.util.Map;

public class GeneMarkerSummaryRefreshResponse {

    private String datasetId;
    private Long deletedRows;
    private Long insertedRows;
    private Long totalRowsAfterRefresh;
    private Long distinctGenes;
    private Map<String, Long> domainCounts;
    private Map<String, Long> signalTypeCounts;
    private int skippedDatasets;
    private int totalDatasets;
    private Long enrichmentStatsRows;
    private Long enrichmentStatsElapsedMillis;
    private Long elapsedMillis;

    public GeneMarkerSummaryRefreshResponse() {
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Long getDeletedRows() {
        return deletedRows;
    }

    public void setDeletedRows(Long deletedRows) {
        this.deletedRows = deletedRows;
    }

    public Long getInsertedRows() {
        return insertedRows;
    }

    public void setInsertedRows(Long insertedRows) {
        this.insertedRows = insertedRows;
    }

    public Long getTotalRowsAfterRefresh() {
        return totalRowsAfterRefresh;
    }

    public void setTotalRowsAfterRefresh(Long totalRowsAfterRefresh) {
        this.totalRowsAfterRefresh = totalRowsAfterRefresh;
    }

    public Long getDistinctGenes() {
        return distinctGenes;
    }

    public void setDistinctGenes(Long distinctGenes) {
        this.distinctGenes = distinctGenes;
    }

    public Map<String, Long> getDomainCounts() {
        return domainCounts;
    }

    public void setDomainCounts(Map<String, Long> domainCounts) {
        this.domainCounts = domainCounts;
    }

    public Map<String, Long> getSignalTypeCounts() {
        return signalTypeCounts;
    }

    public void setSignalTypeCounts(Map<String, Long> signalTypeCounts) {
        this.signalTypeCounts = signalTypeCounts;
    }

    public int getSkippedDatasets() { return skippedDatasets; }
    public void setSkippedDatasets(int v) { this.skippedDatasets = v; }
    public int getTotalDatasets() { return totalDatasets; }
    public void setTotalDatasets(int v) { this.totalDatasets = v; }

    public Long getEnrichmentStatsRows() {
        return enrichmentStatsRows;
    }

    public void setEnrichmentStatsRows(Long enrichmentStatsRows) {
        this.enrichmentStatsRows = enrichmentStatsRows;
    }

    public Long getEnrichmentStatsElapsedMillis() {
        return enrichmentStatsElapsedMillis;
    }

    public void setEnrichmentStatsElapsedMillis(Long enrichmentStatsElapsedMillis) {
        this.enrichmentStatsElapsedMillis = enrichmentStatsElapsedMillis;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }
}
