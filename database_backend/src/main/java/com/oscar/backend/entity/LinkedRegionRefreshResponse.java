package com.oscar.backend.entity;

public class LinkedRegionRefreshResponse {

    private String datasetId;
    private String domain;
    private Long deletedRows;
    private Long insertedRows;
    private String message;

    public LinkedRegionRefreshResponse() {
    }

    public LinkedRegionRefreshResponse(
            String datasetId,
            String domain,
            Long deletedRows,
            Long insertedRows,
            String message
    ) {
        this.datasetId = datasetId;
        this.domain = domain;
        this.deletedRows = deletedRows;
        this.insertedRows = insertedRows;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
