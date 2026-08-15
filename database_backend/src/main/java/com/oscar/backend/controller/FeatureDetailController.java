package com.oscar.backend.controller;

import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsSourcesResponse;
import com.oscar.backend.entity.FeatureOccurrenceResponse;
import com.oscar.backend.service.BedtoolsQueryService;
import com.oscar.backend.service.BedtoolsTrackResolver;
import com.oscar.backend.service.FeatureOccurrenceException;
import com.oscar.backend.service.FeatureOccurrenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feature-detail")
public class FeatureDetailController {

    private static final Logger log = LoggerFactory.getLogger(FeatureDetailController.class);
    private final FeatureOccurrenceService featureOccurrenceService;
    private final BedtoolsTrackResolver bedtoolsTrackResolver;
    private final BedtoolsQueryService bedtoolsQueryService;

    public FeatureDetailController(
            FeatureOccurrenceService featureOccurrenceService,
            BedtoolsTrackResolver bedtoolsTrackResolver,
            BedtoolsQueryService bedtoolsQueryService
    ) {
        this.featureOccurrenceService = featureOccurrenceService;
        this.bedtoolsTrackResolver = bedtoolsTrackResolver;
        this.bedtoolsQueryService = bedtoolsQueryService;
    }

    @GetMapping("/occurrence")
    public FeatureOccurrenceResponse getOccurrence(
            @RequestParam String type,
            @RequestParam(required = false) String gene,
            @RequestParam(required = false) String chrom,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String strand,
            @RequestParam(defaultValue = "integration") String domain,
            @RequestParam(defaultValue = "false") boolean contextOnly,
            @RequestParam(defaultValue = "false") boolean full
    ) {
        long started = System.nanoTime();
        try {
            FeatureOccurrenceResponse response = featureOccurrenceService.getOccurrence(
                    type, gene, chrom, start, end, strand, domain, contextOnly, full
            );
            log.info(
                    "Feature detail request probe endpoint=occurrence type={} gene={} domain={} contextOnly={} "
                            + "providedRegion={} available={} totalOccurrences={} overallMillis={}",
                    type,
                    gene,
                    domain,
                    contextOnly,
                    chrom != null && start != null && end != null,
                    response != null && response.isAvailable(),
                    response != null ? response.getTotalOccurrences() : null,
                    elapsedMillis(started)
            );
            return response;
        } catch (RuntimeException exception) {
            log.info(
                    "Feature detail request probe endpoint=occurrence type={} gene={} domain={} status=failed "
                            + "errorType={} overallMillis={}",
                    type,
                    gene,
                    domain,
                    exception.getClass().getSimpleName(),
                    elapsedMillis(started)
            );
            throw exception;
        }
    }

    @GetMapping("/enhancer-regions")
    public List<Map<String, Object>> getEnhancerRegions(
            @RequestParam String gene,
            @RequestParam(defaultValue = "SE") String enhancerType
    ) {
        return featureOccurrenceService.getEnhancerRegions(gene, enhancerType);
    }

    @GetMapping("/regulatory/gene-annotation")
    public BedtoolsIntersectResponse getGeneRegulatoryAnnotation(
            @RequestParam String gene,
            @RequestParam(required = false) String chrom,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String strand,
            @RequestParam(defaultValue = "gene_body") String mode,
            @RequestParam(required = false) String annotationType,
            @RequestParam(defaultValue = "integration") String domain,
            @RequestParam(defaultValue = "hg38") String genomeBuild
    ) {
        long started = System.nanoTime();
        try {
            BedtoolsIntersectResponse response = featureOccurrenceService.getGeneRegulatoryAnnotation(
                    gene,
                    chrom,
                    start,
                    end,
                    strand,
                    mode,
                    annotationType,
                    domain,
                    genomeBuild
            );
            log.info(
                    "Feature detail request probe endpoint=gene-regulatory gene={} domain={} mode={} annotationType={} "
                            + "providedRegion={} total={} overallMillis={}",
                    gene,
                    domain,
                    mode,
                    annotationType,
                    chrom != null && start != null && end != null,
                    response != null ? response.getTotal() : null,
                    elapsedMillis(started)
            );
            return response;
        } catch (RuntimeException exception) {
            log.info(
                    "Feature detail request probe endpoint=gene-regulatory gene={} domain={} mode={} annotationType={} "
                            + "status=failed errorType={} overallMillis={}",
                    gene,
                    domain,
                    mode,
                    annotationType,
                    exception.getClass().getSimpleName(),
                    elapsedMillis(started)
            );
            throw exception;
        }
    }

    @GetMapping("/expression")
    public List<Map<String, Object>> getExpression(
            @RequestParam String gene,
            @RequestParam String platform,
            @RequestParam(defaultValue = "false") boolean full
    ) {
        long started = System.nanoTime();
        try {
            List<Map<String, Object>> rows = featureOccurrenceService.getExpression(gene, platform, full);
            log.info(
                    "Feature detail request probe endpoint=expression gene={} platform={} full={} rows={} overallMillis={}",
                    gene,
                    platform,
                    full,
                    rows != null ? rows.size() : 0,
                    elapsedMillis(started)
            );
            return rows;
        } catch (RuntimeException exception) {
            log.info(
                    "Feature detail request probe endpoint=expression gene={} platform={} full={} status=failed errorType={} overallMillis={}",
                    gene,
                    platform,
                    full,
                    exception.getClass().getSimpleName(),
                    elapsedMillis(started)
            );
            throw exception;
        }
    }

    @GetMapping("/regulatory/reference-sources")
    public BedtoolsSourcesResponse getReferenceSources(
            @RequestParam(defaultValue = "hg38") String genomeBuild
    ) {
        long started = System.nanoTime();
        BedtoolsSourcesResponse response = new BedtoolsSourcesResponse();
        response.setGenomeBuild(genomeBuild);
        response.setDomain("reference");
        response.setCoordinateSystem("BED 0-based half-open");
        response.setSources(bedtoolsTrackResolver.listReferenceSources(genomeBuild));
        log.info(
                "Feature detail request probe endpoint=reference-sources genomeBuild={} overallMillis={}",
                genomeBuild,
                elapsedMillis(started)
        );
        return response;
    }

    @PostMapping("/regulatory/reference-intersect")
    public BedtoolsIntersectResponse referenceIntersect(
            @RequestParam(defaultValue = "hg38") String genomeBuild,
            @RequestBody BedtoolsIntersectRequest request
    ) {
        return bedtoolsQueryService.referenceIntersect(genomeBuild, request);
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

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
