package com.oscar.backend.entity;

public class BedtoolsSourceOption {

    private String type;
    private String label;
    private String scope;
    private boolean available;
    private String status;
    private String reason;
    private String description;

    public BedtoolsSourceOption() {
    }

    public BedtoolsSourceOption(
            String type,
            String label,
            String scope,
            boolean available,
            String status,
            String reason,
            String description
    ) {
        this.type = type;
        this.label = label;
        this.scope = scope;
        this.available = available;
        this.status = status;
        this.reason = reason;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
