package com.oscar.backend.service;

import com.oscar.backend.entity.DownloadSampleResponse;
import com.oscar.backend.entity.DownloadSampleResponse.DownloadSampleItem;
import com.oscar.backend.mapper.DownloadMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DownloadService {

    private final DownloadMapper downloadMapper;

    public DownloadService(DownloadMapper downloadMapper) {
        this.downloadMapper = downloadMapper;
    }

    public DownloadSampleResponse listDownloadSamples() {
        List<Map<String, Object>> rows = downloadMapper.listDownloadSamples();
        List<DownloadSampleItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            DownloadSampleItem item = new DownloadSampleItem();
            item.setDatasetId(str(row.get("dataset_id")));
            item.setSampleName(str(row.get("sample_name")));
            item.setSampleType(str(row.get("sample_type")));
            item.setTissue(str(row.get("tissue")));
            item.setDisease(str(row.get("disease")));
            item.setSampleSource(str(row.get("sample_source")));
            item.setCellCount(intVal(row.get("cell_count")));
            item.setPlatform(str(row.get("platform")));
            item.setSourceId(str(row.get("source_id")));
            items.add(item);
        }
        DownloadSampleResponse resp = new DownloadSampleResponse();
        resp.setTotal(items.size());
        resp.setItems(items);
        return resp;
    }

    public List<Map<String, Object>> queryMarkerGenes(String datasetId, String domain, String signalType) {
        return downloadMapper.selectMarkerGenes(datasetId, domain, signalType);
    }

    public String normalizeMarkerSignalType(String signalType) {
        if (signalType == null || signalType.isBlank()) return null;
        if ("gene_exp".equalsIgnoreCase(signalType) || "gene_expression".equalsIgnoreCase(signalType)) {
            return "gene_expression";
        }
        if ("gene_score".equalsIgnoreCase(signalType)) return "gene_score";
        return null;
    }

    public List<Map<String, Object>> queryMarkerPeaks(String datasetId, String domain) {
        return downloadMapper.selectMarkerPeaks(datasetId, domain);
    }

    public List<Map<String, Object>> queryP2gLinks(String datasetId, String domain) {
        return downloadMapper.selectP2gLinks(datasetId, domain);
    }

    public List<Map<String, Object>> queryP2gMarkerLinks(String datasetId, String domain) {
        return downloadMapper.selectP2gMarkerLinks(datasetId, domain);
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }

    private static Integer intVal(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        return null;
    }
}
