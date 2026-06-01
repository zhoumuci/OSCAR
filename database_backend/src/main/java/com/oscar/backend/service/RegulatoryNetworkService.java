package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryNetworkExpansionResponse;
import com.oscar.backend.entity.RegulatoryNetworkLinkPageResponse;
import com.oscar.backend.entity.RegulatoryNetworkResponse;

public interface RegulatoryNetworkService {

    RegulatoryNetworkResponse getRegulatoryNetwork(
            String datasetId,
            String domain,
            String mode,
            String gene,
            String peak,
            String groupBy,
            Double minScore,
            Integer maxDistance,
            Integer maxNodes,
            Integer maxEdges
    );

    RegulatoryNetworkExpansionResponse expandRegulatoryNetwork(
            String datasetId,
            String domain,
            String nodeId,
            String nodeType,
            String groupBy,
            Double minScore,
            Integer maxDistance,
            Integer maxNeighbors
    );

    RegulatoryNetworkLinkPageResponse getRegulatoryNetworkLinks(
            String datasetId,
            String domain,
            String nodeType,
            String nodeId,
            Integer page,
            Integer pageSize,
            Double minScore,
            String groupBy
    );
}
