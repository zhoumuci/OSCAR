package com.oscar.backend.entity;

import java.time.LocalDateTime;

public class BedtoolsTrackSourceSummary {

    private Long recordCount;
    private LocalDateTime maxUpdatedAt;

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public LocalDateTime getMaxUpdatedAt() {
        return maxUpdatedAt;
    }

    public void setMaxUpdatedAt(LocalDateTime maxUpdatedAt) {
        this.maxUpdatedAt = maxUpdatedAt;
    }
}
