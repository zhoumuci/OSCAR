package com.oscar.backend.controller;

import com.oscar.backend.entity.ReferenceTrackErrorResponse;
import com.oscar.backend.entity.ReferenceTrackRefreshResponse;
import com.oscar.backend.entity.ReferenceTrackStatusResponse;
import com.oscar.backend.service.ReferenceTrackException;
import com.oscar.backend.service.ReferenceTrackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reference-tracks")
public class ReferenceTrackAdminController {

    private final ReferenceTrackService referenceTrackService;

    public ReferenceTrackAdminController(ReferenceTrackService referenceTrackService) {
        this.referenceTrackService = referenceTrackService;
    }

    @PostMapping("/refresh")
    public ReferenceTrackRefreshResponse refreshReferenceTracks(
            @RequestParam(defaultValue = "hg38") String genomeBuild,
            @RequestParam(defaultValue = "regulatory") String category
    ) {
        return referenceTrackService.refreshReferenceTracks(genomeBuild, category);
    }

    @GetMapping("/status")
    public ReferenceTrackStatusResponse getReferenceTrackStatus(
            @RequestParam(defaultValue = "hg38") String genomeBuild,
            @RequestParam(defaultValue = "regulatory") String category
    ) {
        return referenceTrackService.getReferenceTrackStatus(genomeBuild, category);
    }

    @ExceptionHandler(ReferenceTrackException.class)
    public ResponseEntity<ReferenceTrackErrorResponse> handleReferenceTrackException(
            ReferenceTrackException exception
    ) {
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(new ReferenceTrackErrorResponse(exception.getStatus(), exception.getMessage()));
    }
}
