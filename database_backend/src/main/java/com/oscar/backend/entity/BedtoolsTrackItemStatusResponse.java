package com.oscar.backend.entity;

public class BedtoolsTrackItemStatusResponse {

    private String trackType;
    private String status;
    private String trackPath;
    private Long recordCount;
    private Long skippedCount;
    private String errorMessage;

    public BedtoolsTrackItemStatusResponse() {
    }

    public BedtoolsTrackItemStatusResponse(
            String trackType,
            String status,
            String trackPath,
            Long recordCount,
            Long skippedCount,
            String errorMessage
    ) {
        this.trackType = trackType;
        this.status = status;
        this.trackPath = trackPath;
        this.recordCount = recordCount;
        this.skippedCount = skippedCount;
        this.errorMessage = errorMessage;
    }

    public String getTrackType() {
        return trackType;
    }

    public void setTrackType(String trackType) {
        this.trackType = trackType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackPath() {
        return trackPath;
    }

    public void setTrackPath(String trackPath) {
        this.trackPath = trackPath;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Long getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Long skippedCount) {
        this.skippedCount = skippedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
