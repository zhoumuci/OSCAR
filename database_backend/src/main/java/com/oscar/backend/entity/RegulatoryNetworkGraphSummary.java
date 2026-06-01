package com.oscar.backend.entity;

public class RegulatoryNetworkGraphSummary {

    private String anchorNodeId;
    private String anchorNodeType;
    private Long totalLinks;
    private Long returnedLinks;
    private Integer graphLimit;
    private Boolean hasMoreLinks;

    public RegulatoryNetworkGraphSummary() {
    }

    public RegulatoryNetworkGraphSummary(
            String anchorNodeId,
            String anchorNodeType,
            Long totalLinks,
            Long returnedLinks,
            Integer graphLimit,
            Boolean hasMoreLinks
    ) {
        this.anchorNodeId = anchorNodeId;
        this.anchorNodeType = anchorNodeType;
        this.totalLinks = totalLinks;
        this.returnedLinks = returnedLinks;
        this.graphLimit = graphLimit;
        this.hasMoreLinks = hasMoreLinks;
    }

    public String getAnchorNodeId() {
        return anchorNodeId;
    }

    public void setAnchorNodeId(String anchorNodeId) {
        this.anchorNodeId = anchorNodeId;
    }

    public String getAnchorNodeType() {
        return anchorNodeType;
    }

    public void setAnchorNodeType(String anchorNodeType) {
        this.anchorNodeType = anchorNodeType;
    }

    public Long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(Long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public Long getReturnedLinks() {
        return returnedLinks;
    }

    public void setReturnedLinks(Long returnedLinks) {
        this.returnedLinks = returnedLinks;
    }

    public Integer getGraphLimit() {
        return graphLimit;
    }

    public void setGraphLimit(Integer graphLimit) {
        this.graphLimit = graphLimit;
    }

    public Boolean getHasMoreLinks() {
        return hasMoreLinks;
    }

    public void setHasMoreLinks(Boolean hasMoreLinks) {
        this.hasMoreLinks = hasMoreLinks;
    }
}
