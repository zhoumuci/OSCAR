package com.oscar.backend.entity;

import java.util.List;

public class BrowseSamplePageResponse {

    private List<BrowseSampleResponse> records;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public BrowseSamplePageResponse() {
    }

    public BrowseSamplePageResponse(List<BrowseSampleResponse> records, Long total, Integer page, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<BrowseSampleResponse> getRecords() {
        return records;
    }

    public void setRecords(List<BrowseSampleResponse> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
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
