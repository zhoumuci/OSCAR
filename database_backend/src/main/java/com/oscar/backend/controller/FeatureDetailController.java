package com.oscar.backend.controller;

import com.oscar.backend.entity.FeatureOccurrenceResponse;
import com.oscar.backend.service.FeatureOccurrenceException;
import com.oscar.backend.service.FeatureOccurrenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feature-detail")
public class FeatureDetailController {

    private final FeatureOccurrenceService featureOccurrenceService;

    public FeatureDetailController(FeatureOccurrenceService featureOccurrenceService) {
        this.featureOccurrenceService = featureOccurrenceService;
    }

    @GetMapping("/occurrence")
    public FeatureOccurrenceResponse getOccurrence(
            @RequestParam String type,
            @RequestParam(required = false) String gene,
            @RequestParam(required = false) String chrom,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "integration") String domain,
            @RequestParam(defaultValue = "false") boolean contextOnly
    ) {
        return featureOccurrenceService.getOccurrence(type, gene, chrom, start, end, domain, contextOnly);
    }

    @ExceptionHandler(FeatureOccurrenceException.class)
    public ResponseEntity<Map<String, String>> handleFeatureOccurrenceException(
            FeatureOccurrenceException exception
    ) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", exception.getStatus());
        body.put("message", exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(body);
    }
}
