package com.oscar.backend.entity;

import java.util.List;

public class RegulatoryNetworkLinkPageResponse {

    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<RegulatoryNetworkLink> items;

    public RegulatoryNetworkLinkPageResponse() {
    }

    public RegulatoryNetworkLinkPageResponse(Long total, Integer page, Integer pageSize, List<RegulatoryNetworkLink> items) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.items = items;
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

    public List<RegulatoryNetworkLink> getItems() {
        return items;
    }

    public void setItems(List<RegulatoryNetworkLink> items) {
        this.items = items;
    }
}
