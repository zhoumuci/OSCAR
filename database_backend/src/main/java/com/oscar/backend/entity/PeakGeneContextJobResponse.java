package com.oscar.backend.entity;

import java.time.Instant;

public class PeakGeneContextJobResponse {

    private String jobId;
    private String status;
    private int progress;
    private String stage;
    private String message;
    private String error;
    private long pollAfterMs = 1000L;
    private Instant createdAt;
    private Instant updatedAt;
    private PeakGeneContextResponse result;

    public String getJobId() { return jobId; }
    public void setJobId(String v) { this.jobId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public int getProgress() { return progress; }
    public void setProgress(int v) { this.progress = v; }
    public String getStage() { return stage; }
    public void setStage(String v) { this.stage = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getError() { return error; }
    public void setError(String v) { this.error = v; }
    public long getPollAfterMs() { return pollAfterMs; }
    public void setPollAfterMs(long v) { this.pollAfterMs = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public PeakGeneContextResponse getResult() { return result; }
    public void setResult(PeakGeneContextResponse v) { this.result = v; }
}
