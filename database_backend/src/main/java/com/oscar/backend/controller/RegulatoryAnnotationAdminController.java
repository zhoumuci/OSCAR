package com.oscar.backend.controller;

import com.oscar.backend.entity.LinkedRegionRefreshResponse;
import com.oscar.backend.service.RegulatoryAnnotationMaintenanceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/regulatory-annotation")
public class RegulatoryAnnotationAdminController {

    private final RegulatoryAnnotationMaintenanceService regulatoryAnnotationMaintenanceService;

    public RegulatoryAnnotationAdminController(
            RegulatoryAnnotationMaintenanceService regulatoryAnnotationMaintenanceService
    ) {
        this.regulatoryAnnotationMaintenanceService = regulatoryAnnotationMaintenanceService;
    }

    @PostMapping("/linked-regions/refresh")
    public LinkedRegionRefreshResponse refreshLinkedRegions(
            @RequestParam String datasetId,
            @RequestParam(required = false) String domain
    ) {
        return regulatoryAnnotationMaintenanceService.refreshMarkerLinkedRegions(datasetId, domain);
    }
}
