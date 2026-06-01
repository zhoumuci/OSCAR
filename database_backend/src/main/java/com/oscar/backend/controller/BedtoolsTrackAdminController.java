package com.oscar.backend.controller;

import com.oscar.backend.entity.BedtoolsTrackBuildAllResponse;
import com.oscar.backend.entity.BedtoolsTrackStatusResponse;
import com.oscar.backend.service.BedtoolsTrackBuildService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bedtools/tracks")
public class BedtoolsTrackAdminController {

    private final BedtoolsTrackBuildService bedtoolsTrackBuildService;

    public BedtoolsTrackAdminController(BedtoolsTrackBuildService bedtoolsTrackBuildService) {
        this.bedtoolsTrackBuildService = bedtoolsTrackBuildService;
    }

    @GetMapping("/status")
    public BedtoolsTrackStatusResponse getTrackStatus(
            @RequestParam String datasetId,
            @RequestParam String domain,
            @RequestParam(defaultValue = "hg38") String genomeBuild
    ) {
        return bedtoolsTrackBuildService.getTrackStatus(datasetId, domain, genomeBuild);
    }

    @PostMapping("/build")
    public BedtoolsTrackStatusResponse buildSampleTracks(
            @RequestParam String datasetId,
            @RequestParam String domain,
            @RequestParam(defaultValue = "hg38") String genomeBuild,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return bedtoolsTrackBuildService.buildSampleTracks(datasetId, domain, genomeBuild, force);
    }

    @PostMapping("/build-all")
    public BedtoolsTrackBuildAllResponse buildAllSampleTracks(
            @RequestParam(defaultValue = "integration") String domain,
            @RequestParam(defaultValue = "hg38") String genomeBuild,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return bedtoolsTrackBuildService.buildAllSampleTracks(domain, genomeBuild, force);
    }
}
