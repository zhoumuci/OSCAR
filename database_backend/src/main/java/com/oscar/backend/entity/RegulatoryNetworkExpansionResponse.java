package com.oscar.backend.entity;

import java.util.List;

public class RegulatoryNetworkExpansionResponse extends RegulatoryNetworkResponse {

    private Long totalNeighbors;

    public RegulatoryNetworkExpansionResponse() {
    }

    public RegulatoryNetworkExpansionResponse(
            List<RegulatoryNetworkNode> nodes,
            List<RegulatoryNetworkEdge> edges,
            List<RegulatoryNetworkLink> links,
            Long totalLinks,
            Boolean hasMore,
            Long totalNeighbors
    ) {
        super(nodes, edges, links, totalLinks, hasMore);
        this.totalNeighbors = totalNeighbors;
    }

    public Long getTotalNeighbors() {
        return totalNeighbors;
    }

    public void setTotalNeighbors(Long totalNeighbors) {
        this.totalNeighbors = totalNeighbors;
    }
}
