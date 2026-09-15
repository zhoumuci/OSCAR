package com.oscar.backend.controller;

import com.oscar.backend.entity.DownloadSampleResponse;
import com.oscar.backend.service.BulkDownloadAuthorizationService;
import com.oscar.backend.service.DownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/download")
public class DownloadController {

    private final DownloadService downloadService;
    private final BulkDownloadAuthorizationService bulkDownloadAuthorizationService;

    public DownloadController(
            DownloadService downloadService,
            BulkDownloadAuthorizationService bulkDownloadAuthorizationService
    ) {
        this.downloadService = downloadService;
        this.bulkDownloadAuthorizationService = bulkDownloadAuthorizationService;
    }

    @GetMapping("/samples")
    public DownloadSampleResponse listSamples() {
        return downloadService.listDownloadSamples();
    }

    @GetMapping("/{domain}/{datasetId}")
    public void downloadFile(
            @PathVariable String domain,
            @PathVariable String datasetId,
            @RequestParam(defaultValue = "marker_gene") String type,
            @RequestParam(defaultValue = "tsv") String format,
            @RequestParam(required = false) String signalType,
            HttpServletResponse response) throws IOException {

        String markerFileType = downloadService.normalizeMarkerSignalType(signalType);
        writeDownloadFile(domain, datasetId, type, format, markerFileType, response);
    }

    @GetMapping("/cart/challenge")
    public BulkDownloadAuthorizationService.ChallengeResult createBulkDownloadChallenge(HttpServletRequest request) {
        return bulkDownloadAuthorizationService.issueChallenge(request.getSession(true));
    }

    @PostMapping("/cart/authorize")
    public BulkDownloadAuthorizationService.AuthorizationResult authorizeBulkDownload(
            @RequestBody BulkDownloadAuthorizationRequest requestBody,
            HttpServletRequest request
    ) {
        return bulkDownloadAuthorizationService.authorize(
                request.getSession(true),
                requestBody.challengeId(),
                requestBody.answer(),
                requestBody.sampleIds(),
                requestBody.domain(),
                requestBody.type(),
                requestBody.format()
        );
    }

    @GetMapping("/bulk/{domain}/{datasetId}")
    public void downloadVerifiedBulkFile(
            @PathVariable String domain,
            @PathVariable String datasetId,
            @RequestParam(defaultValue = "marker_gene") String type,
            @RequestParam(defaultValue = "tsv") String format,
            @RequestParam(required = false) String signalType,
            @RequestHeader(value = "X-OSCAR-Bulk-Download-Token", required = false) String bulkToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String markerFileType = downloadService.normalizeMarkerSignalType(signalType);
        String authorizationType = authorizationType(type, markerFileType);
        bulkDownloadAuthorizationService.validate(
                request.getSession(false),
                bulkToken,
                datasetId,
                domain,
                authorizationType,
                format
        );
        writeDownloadFile(domain, datasetId, type, format, markerFileType, response);
    }

    private void writeDownloadFile(
            String domain,
            String datasetId,
            String type,
            String format,
            String markerFileType,
            HttpServletResponse response
    ) throws IOException {
        String filenameType = "marker_gene".equals(type) && markerFileType != null ? markerFileType : type;
        String filename = datasetId + "_" + domain + "_" + filenameType + "." + format;
        response.setContentType(format.equals("csv") ? "text/csv; charset=UTF-8" : "text/tab-separated-values; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        List<Map<String, Object>> rows;
        switch (type) {
            case "marker_peak" -> rows = downloadService.queryMarkerPeaks(datasetId, domain);
            case "p2g" -> rows = downloadService.queryP2gLinks(datasetId, domain);
            case "p2g_marker" -> rows = downloadService.queryP2gMarkerLinks(datasetId, domain);
            default -> rows = downloadService.queryMarkerGenes(datasetId, domain, markerFileType);
        }

        char sep = format.equals("csv") ? ',' : '\t';
        try (Writer w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            if (!rows.isEmpty()) {
                // header
                var first = rows.get(0);
                int col = 0;
                for (String key : first.keySet()) {
                    if (col++ > 0) w.write(sep);
                    w.write(key);
                }
                w.write("\n");
                // data
                for (Map<String, Object> row : rows) {
                    col = 0;
                    for (Object val : row.values()) {
                        if (col++ > 0) w.write(sep);
                        if (val != null) {
                            String s = val.toString();
                            if (s.contains(String.valueOf(sep)) || s.contains("\"") || s.contains("\n")) {
                                w.write("\"" + s.replace("\"", "\"\"") + "\"");
                            } else {
                                w.write(s);
                            }
                        }
                    }
                    w.write("\n");
                }
            }
        }
    }

    private static String authorizationType(String type, String markerFileType) {
        if (!"marker_gene".equalsIgnoreCase(type)) return type;
        if ("gene_expression".equals(markerFileType)) return "gene_exp";
        if ("gene_score".equals(markerFileType)) return "gene_score";
        return "marker_gene";
    }

    public record BulkDownloadAuthorizationRequest(
            String challengeId,
            Integer answer,
            List<String> sampleIds,
            String domain,
            String type,
            String format
    ) { }
}
