package com.oscar.backend.controller;

import com.oscar.backend.entity.DownloadSampleResponse;
import com.oscar.backend.service.DownloadService;
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

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
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
}
