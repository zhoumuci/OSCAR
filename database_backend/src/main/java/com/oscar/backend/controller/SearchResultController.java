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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

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
            @RequestParam(required = false) String colorBy,
            @RequestParam(required = false) Integer maxPoints
    ) {
        return searchResultService.getUmap(datasetId, domain, embedding, colorBy, maxPoints);
    }

    @GetMapping(value = "/umap/download", produces = "text/csv")
    public void downloadFullUmap(
            @RequestParam String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String embedding,
            @RequestParam(required = false) String colorBy,
            HttpServletResponse response
    ) throws IOException {
        String domainPart = downloadPart(domain, "integration").toLowerCase();
        String colorFallback = "integration".equals(domainPart) ? "celltype" : "cluster";
        String filename = downloadPart(datasetId, "sample")
                + "_" + domainPart
                + "_" + downloadPart(embedding, "umap").toLowerCase()
                + "_" + downloadPart(colorBy, colorFallback).toLowerCase()
                + "_full.csv";
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        searchResultService.writeFullUmapCsv(datasetId, domain, embedding, colorBy, response.getOutputStream());
    }

    private String downloadPart(String value, String fallback) {
        String candidate = value == null || value.isBlank() ? fallback : value.trim();
        return candidate.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    @GetMapping("/regulatory-network/links")
    public RegulatoryNetworkLinkPageResponse getRegulatoryNetworkLinks(
            @RequestParam String datasetId,
            @RequestParam String domain,
            @RequestParam String nodeType,
            @RequestParam String nodeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String gene,
            @RequestParam(required = false) String peak,
            @RequestParam(required = false) Double minScore
    ) {
        return regulatoryNetworkService.getRegulatoryNetworkLinks(
                datasetId,
                domain,
                nodeType,
                nodeId,
                page,
                pageSize,
                gene,
                peak,
                minScore
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
