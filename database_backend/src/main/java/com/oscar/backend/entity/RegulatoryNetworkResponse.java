package com.oscar.backend.entity;

import java.util.List;

public class RegulatoryNetworkResponse {

    private List<RegulatoryNetworkNode> nodes;
    private List<RegulatoryNetworkEdge> edges;
    private List<RegulatoryNetworkLink> links;
    private Long totalLinks;
    private Boolean hasMore;
    private RegulatoryNetworkGraphSummary summary;

    public RegulatoryNetworkResponse() {
    }

    public RegulatoryNetworkResponse(
            List<RegulatoryNetworkNode> nodes,
            List<RegulatoryNetworkEdge> edges,
            List<RegulatoryNetworkLink> links,
            Long totalLinks,
            Boolean hasMore
    ) {
        this.nodes = nodes;
        this.edges = edges;
        this.links = links;
        this.totalLinks = totalLinks;
        this.hasMore = hasMore;
    }

    public RegulatoryNetworkResponse(
            List<RegulatoryNetworkNode> nodes,
            List<RegulatoryNetworkEdge> edges,
            List<RegulatoryNetworkLink> links,
            Long totalLinks,
            Boolean hasMore,
            RegulatoryNetworkGraphSummary summary
    ) {
        this(nodes, edges, links, totalLinks, hasMore);
        this.summary = summary;
    }

    public List<RegulatoryNetworkNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<RegulatoryNetworkNode> nodes) {
        this.nodes = nodes;
    }

    public List<RegulatoryNetworkEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<RegulatoryNetworkEdge> edges) {
        this.edges = edges;
    }

    public List<RegulatoryNetworkLink> getLinks() {
        return links;
    }

    public void setLinks(List<RegulatoryNetworkLink> links) {
        this.links = links;
    }

    public Long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(Long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public RegulatoryNetworkGraphSummary getSummary() {
        return summary;
    }

    public void setSummary(RegulatoryNetworkGraphSummary summary) {
        this.summary = summary;
    }
}
