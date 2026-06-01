package com.oscar.backend.controller;

import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsSourcesResponse;
import com.oscar.backend.service.BedtoolsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search-result/{datasetId}/regulatory/bedtools")
public class BedtoolsQueryController {

    private final BedtoolsQueryService bedtoolsQueryService;

    public BedtoolsQueryController(BedtoolsQueryService bedtoolsQueryService) {
        this.bedtoolsQueryService = bedtoolsQueryService;
    }

    @GetMapping("/sources")
    public BedtoolsSourcesResponse getSources(
            @PathVariable String datasetId,
            @RequestParam String domain,
            @RequestParam(defaultValue = "hg38") String genomeBuild
    ) {
        return bedtoolsQueryService.getSources(datasetId, domain, genomeBuild);
    }

    @PostMapping("/intersect")
    public BedtoolsIntersectResponse intersect(
            @PathVariable String datasetId,
            @RequestBody BedtoolsIntersectRequest request
    ) {
        return bedtoolsQueryService.intersect(datasetId, request);
    }
}
