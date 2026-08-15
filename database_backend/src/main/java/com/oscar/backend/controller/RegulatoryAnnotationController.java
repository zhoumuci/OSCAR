package com.oscar.backend.controller;

import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.service.RegulatoryAnnotationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/samples/{datasetId}")
public class RegulatoryAnnotationController {

    private final RegulatoryAnnotationService regulatoryAnnotationService;

    public RegulatoryAnnotationController(RegulatoryAnnotationService regulatoryAnnotationService) {
        this.regulatoryAnnotationService = regulatoryAnnotationService;
    }

    @GetMapping("/regulatory-annotations")
    public RegulatoryAnnotationResponse getRegulatoryAnnotations(
            @PathVariable String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String annotationType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String targetGene,
            @RequestParam(required = false) String peak,
            @RequestParam(required = false) String regionType,
            @RequestParam(required = false) String contextCellType,
            @RequestParam(required = false) String contextCluster,
            @RequestParam(required = false) Double maxFdr,
            @RequestParam(required = false) Double minLog2fc,
            @RequestParam(required = false) Double minP2gScore,
            @RequestParam(required = false) String signalType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false, defaultValue = "marker") String p2gMode
    ) {
        return regulatoryAnnotationService.getRegulatoryAnnotations(
                datasetId,
                domain,
                annotationType,
                page,
                pageSize,
                targetGene,
                peak,
                regionType,
                contextCellType,
                contextCluster,
                maxFdr,
                minLog2fc,
                minP2gScore,
                signalType,
                sortBy,
                sortOrder,
                p2gMode
        );
    }

    @GetMapping(value = "/regulatory-annotations/download.csv", produces = "text/csv")
    public void streamRegulatoryAnnotationsCsv(
            @PathVariable String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String annotationType,
            @RequestParam(required = false) String targetGene,
            @RequestParam(required = false) String peak,
            @RequestParam(required = false) String regionType,
            @RequestParam(required = false) String contextCellType,
            @RequestParam(required = false) String contextCluster,
            @RequestParam(required = false) Double maxFdr,
            @RequestParam(required = false) Double minLog2fc,
            @RequestParam(required = false) Double minP2gScore,
            @RequestParam(required = false) String signalType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false, defaultValue = "marker") String p2gMode,
            @RequestParam(required = false) String sampleLabel,
            HttpServletResponse response
    ) throws IOException {
        String normalizedAnnotationType = safeFilenamePart(annotationType, "regulatory_annotations");
        String normalizedP2gMode = "linked_region".equalsIgnoreCase(annotationType)
                ? "_" + ("all".equalsIgnoreCase(p2gMode) ? "all" : "marker")
                : "_all";
        String filename = safeFilenamePart(datasetId, "sample")
                + "_" + safeFilenamePart(domain, "domain")
                + "_" + normalizedAnnotationType + normalizedP2gMode + ".csv";
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setBufferSize(64 * 1024);

        regulatoryAnnotationService.streamRegulatoryAnnotationsCsv(
                datasetId, domain, annotationType, targetGene, peak, regionType,
                contextCellType, contextCluster, maxFdr, minLog2fc, minP2gScore,
                signalType, sortBy, sortOrder, p2gMode, sampleLabel, response.getOutputStream()
        );
    }

    private String safeFilenamePart(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String safe = value.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        return safe.isBlank() ? fallback : safe;
    }

    @GetMapping("/regulatory-annotations/context-options")
    public List<RegulatoryAnnotationContextOption> getRegulatoryAnnotationContextOptions(
            @PathVariable String datasetId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String annotationType
    ) {
        return regulatoryAnnotationService.getRegulatoryAnnotationContextOptions(datasetId, domain, annotationType);
    }
}
