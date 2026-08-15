package com.oscar.backend.entity;

import java.util.Map;

public class LinkedRegionRefreshResponse {

    private String datasetId;
    private String domain;
    private Long deletedRows;
    private Long insertedRows;
    private Long totalRowsAfterRefresh;
    private Long distinctGenes;
    private Long distinctPeaks;
    private Long rowsWithMarkerPeak;
    private Long rowsWithoutMarkerPeak;
    private Map<String, Long> signalTypeCounts;
    private Long elapsedMillis;
    private String message;

    public LinkedRegionRefreshResponse() {
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    public Long getDistinctPeaks() {
        return distinctPeaks;
    }

    public void setDistinctPeaks(Long distinctPeaks) {
        this.distinctPeaks = distinctPeaks;
    }

    public Long getRowsWithMarkerPeak() {
        return rowsWithMarkerPeak;
    }

    public void setRowsWithMarkerPeak(Long rowsWithMarkerPeak) {
        this.rowsWithMarkerPeak = rowsWithMarkerPeak;
    }

    public Long getRowsWithoutMarkerPeak() {
        return rowsWithoutMarkerPeak;
    }

    public void setRowsWithoutMarkerPeak(Long rowsWithoutMarkerPeak) {
        this.rowsWithoutMarkerPeak = rowsWithoutMarkerPeak;
    }

    public Map<String, Long> getSignalTypeCounts() {
        return signalTypeCounts;
    }

    public void setSignalTypeCounts(Map<String, Long> signalTypeCounts) {
        this.signalTypeCounts = signalTypeCounts;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
