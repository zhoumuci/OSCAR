package com.oscar.backend.controller;

import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.service.RegulatoryAnnotationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(required = false) Double minLog2fc
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
                minLog2fc
        );
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
