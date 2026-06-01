package com.oscar.backend.controller;

import com.oscar.backend.entity.SearchResultCellTypeCompositionResponse;
import com.oscar.backend.entity.SearchResultOverviewResponse;
import com.oscar.backend.entity.SearchResultQcViolinResponse;
import com.oscar.backend.entity.SearchResultUmapResponse;
import com.oscar.backend.entity.RegulatoryNetworkLinkPageResponse;
import com.oscar.backend.entity.RegulatoryTfSummaryResponse;
import com.oscar.backend.service.RegulatoryNetworkService;
import com.oscar.backend.service.RegulatoryAnnotationService;
import com.oscar.backend.service.SearchResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search-result")
public class SearchResultController {

    private final SearchResultService searchResultService;
    private final RegulatoryNetworkService regulatoryNetworkService;
    private final RegulatoryAnnotationService regulatoryAnnotationService;

    public SearchResultController(
            SearchResultService searchResultService,
            RegulatoryNetworkService regulatoryNetworkService,
            RegulatoryAnnotationService regulatoryAnnotationService
    ) {
        this.searchResultService = searchResultService;
        this.regulatoryNetworkService = regulatoryNetworkService;
        this.regulatoryAnnotationService = regulatoryAnnotationService;
    }

    @GetMapping("/overview")
    public SearchResultOverviewResponse getOverview(@RequestParam String datasetId) {
        return searchResultService.getOverview(datasetId);
    }

    @GetMapping("/celltype-composition")
    public SearchResultCellTypeCompositionResponse getCellTypeComposition(
            @RequestParam String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false, defaultValue = "celltype") String groupBy
    ) {
        return searchResultService.getCellTypeComposition(datasetId, domain, groupBy);
    }

    @GetMapping("/qc-violin")
    public SearchResultQcViolinResponse getQcViolin(
            @RequestParam String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false, defaultValue = "celltype") String groupBy,
            @RequestParam(required = false) String metrics
    ) {
        return searchResultService.getQcViolin(datasetId, domain, groupBy, metrics);
    }

    @GetMapping("/umap")
    public SearchResultUmapResponse getUmap(
            @RequestParam String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String embedding,
            @RequestParam(required = false, defaultValue = "celltype") String colorBy,
            @RequestParam(required = false) Integer maxPoints
    ) {
        return searchResultService.getUmap(datasetId, domain, embedding, colorBy, maxPoints);
    }

    @GetMapping("/regulatory-network/links")
    public RegulatoryNetworkLinkPageResponse getRegulatoryNetworkLinks(
            @RequestParam String datasetId,
            @RequestParam String domain,
            @RequestParam String nodeType,
            @RequestParam String nodeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) String groupBy
    ) {
        return regulatoryNetworkService.getRegulatoryNetworkLinks(
                datasetId,
                domain,
                nodeType,
                nodeId,
                page,
                pageSize,
                minScore,
                groupBy
        );
    }

    @GetMapping("/{datasetId}/regulatory/tf-summary")
    public RegulatoryTfSummaryResponse getRegulatoryTfSummary(
            @PathVariable String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam String featureType,
            @RequestParam(required = false) String gene,
            @RequestParam(required = false) String chrom,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String peakId
    ) {
        return regulatoryAnnotationService.getTfSummary(datasetId, domain, featureType, gene, chrom, start, end, peakId);
    }
}
