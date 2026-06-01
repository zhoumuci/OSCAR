package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class BedtoolsIntersectRequest {

    private String domain;
    private String genomeBuild;
    private String region;
    private List<String> annotationTypes = new ArrayList<>();
    private Integer minOverlapBp;
    private Integer page;
    private Integer pageSize;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<String> getAnnotationTypes() {
        return annotationTypes;
    }

    public void setAnnotationTypes(List<String> annotationTypes) {
        this.annotationTypes = annotationTypes;
    }

    public Integer getMinOverlapBp() {
        return minOverlapBp;
    }

    public void setMinOverlapBp(Integer minOverlapBp) {
        this.minOverlapBp = minOverlapBp;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
