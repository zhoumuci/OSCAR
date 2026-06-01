package com.oscar.backend.controller;

import com.oscar.backend.entity.RegulatoryNetworkExpansionResponse;
import com.oscar.backend.entity.RegulatoryNetworkResponse;
import com.oscar.backend.service.RegulatoryNetworkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/samples/{datasetId}")
public class RegulatoryNetworkController {

    private final RegulatoryNetworkService regulatoryNetworkService;

    public RegulatoryNetworkController(RegulatoryNetworkService regulatoryNetworkService) {
        this.regulatoryNetworkService = regulatoryNetworkService;
    }

    @GetMapping("/regulatory-network")
    public RegulatoryNetworkResponse getRegulatoryNetwork(
            @PathVariable String datasetId,
            @RequestParam String domain,
            @RequestParam String mode,
            @RequestParam(required = false) String gene,
            @RequestParam(required = false) String peak,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Integer maxDistance,
            @RequestParam(required = false) Integer maxNodes,
            @RequestParam(required = false) Integer maxEdges
    ) {
        return regulatoryNetworkService.getRegulatoryNetwork(
                datasetId,
                domain,
                mode,
                gene,
                peak,
                groupBy,
                minScore,
                maxDistance,
                maxNodes,
                maxEdges
        );
    }

    @GetMapping("/regulatory-network/expand")
    public RegulatoryNetworkExpansionResponse expandRegulatoryNetwork(
            @PathVariable String datasetId,
            @RequestParam String domain,
            @RequestParam String nodeId,
            @RequestParam String nodeType,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Integer maxDistance,
            @RequestParam(required = false) Integer maxNeighbors
    ) {
        return regulatoryNetworkService.expandRegulatoryNetwork(
                datasetId,
                domain,
                nodeId,
                nodeType,
                groupBy,
                minScore,
                maxDistance,
                maxNeighbors
        );
    }
}
