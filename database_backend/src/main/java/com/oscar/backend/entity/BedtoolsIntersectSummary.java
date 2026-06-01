package com.oscar.backend.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class BedtoolsIntersectSummary {

    private Long totalHits;
    private Map<String, Long> byAnnotationType = new LinkedHashMap<>();
    private Long elapsedMillis;

    public Long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Long totalHits) {
        this.totalHits = totalHits;
    }

    public Map<String, Long> getByAnnotationType() {
        return byAnnotationType;
    }

    public void setByAnnotationType(Map<String, Long> byAnnotationType) {
        this.byAnnotationType = byAnnotationType;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }
}
