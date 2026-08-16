package com.oscar.backend.entity;

public class BrowseSampleQuery {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private String keyword;
    private String species;
    private String sampleType;
    private String tissue;
    private Integer page = DEFAULT_PAGE;
    private Integer pageSize = DEFAULT_PAGE_SIZE;
    private String sortBy;
    private String sortDir;

    public void normalize() {
        keyword = trimToNull(keyword);
        species = trimToNull(species);
        sampleType = trimToNull(sampleType);
        tissue = trimToNull(tissue);

        if (page == null || page < 1) {
            page = DEFAULT_PAGE;
        }

        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        // Only allow whitelisted sort columns and directions
        sortBy = normalizeSortBy(sortBy);
        sortDir = normalizeSortDir(sortDir);
    }

    public String getSortColumn() {
        return switch (sortBy) {
            case "cells" -> "cell_count";
            case "sampleName" -> "sample_name";
            case "sampleType" -> "sample_type";
            case "tissue" -> "tissue";
            case "platform" -> "platform";
            case "sourceId" -> "source_id";
            case "disease" -> "disease";
            case "sampleSource" -> "sample_source";
            // dataset_id 为定长零填充格式（H_000001），文本序即数值序
            default -> "dataset_id";
        };
    }

    public String getSortDirection() {
        return "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
    }

    private String normalizeSortBy(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return "datasetId";
        return switch (trimmed.toLowerCase()) {
            case "cells", "cell_count" -> "cells";
            case "samplename", "sample_name" -> "sampleName";
            case "sampletype", "sample_type" -> "sampleType";
            case "tissue" -> "tissue";
            case "platform" -> "platform";
            case "sourceid", "source_id" -> "sourceId";
            case "disease" -> "disease";
            case "samplesource", "sample_source" -> "sampleSource";
            default -> "datasetId";
        };
    }

    private String normalizeSortDir(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return "asc";
        return switch (trimmed.toLowerCase()) {
            case "desc", "descending", "descend" -> "desc";
            default -> "asc";
        };
    }

    public int getOffset() {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return (normalizedPage - 1) * normalizedPageSize;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }
}
