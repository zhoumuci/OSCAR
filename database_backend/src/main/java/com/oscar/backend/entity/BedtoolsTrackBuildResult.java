package com.oscar.backend.entity;

public class BedtoolsTrackBuildResult {

    private String datasetId;
    private String status;
    private String action;
    private String message;
    private String errorMessage;

    public BedtoolsTrackBuildResult() {
    }

    public BedtoolsTrackBuildResult(String datasetId, String status, String action, String message, String errorMessage) {
        this.datasetId = datasetId;
        this.status = status;
        this.action = action;
        this.message = message;
        this.errorMessage = errorMessage;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
