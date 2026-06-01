package com.oscar.backend.service;

import com.oscar.backend.entity.FeatureOccurrenceResponse;
import com.oscar.backend.entity.FeatureOccurrenceResponse.CellContextRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.DatasetRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceDatasetEntry;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceTopCellType;
import com.oscar.backend.mapper.FeatureOccurrenceMapper;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.FeatureOccurrenceAggregation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
@Service
public class FeatureOccurrenceService {

    private static final String DEFAULT_DOMAIN = "integration";
    private static final int DEFAULT_RANKING_LIMIT = 10;

    private final FeatureOccurrenceMapper mapper;

    public FeatureOccurrenceService(FeatureOccurrenceMapper mapper) {
        this.mapper = mapper;
    }

    public FeatureOccurrenceResponse getOccurrence(
            String type, String gene, String chrom, Long start, Long end, String domain, boolean contextOnly) {
        String nt = normalizeType(type);
        String nd = normalizeDomain(domain);
        if ("gene".equals(nt)) {
            if (isBlank(gene)) throw new FeatureOccurrenceException("INVALID_PARAMETER", "gene is required when type=gene", HttpStatus.BAD_REQUEST);
            return buildGeneResponse(gene.trim().toUpperCase(Locale.ROOT), nd);
        }
        if (isBlank(chrom) || start == null || end == null)
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "chrom, start, end are required when type=peak", HttpStatus.BAD_REQUEST);
        if (start < 0 || end <= start)
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "start must be >=0 and end > start", HttpStatus.BAD_REQUEST);
        return buildPeakResponse(chrom.trim(), start, end, nd);
    }

    private FeatureOccurrenceResponse buildGeneResponse(String gene, String domain) {
        FeatureOccurrenceAggregation agg = mapper.selectGeneAggregation(gene, domain);
        List<FeatureOccurrenceTopCellType> top = mapper.selectGeneTopCellTypes(gene, domain);
        List<FeatureOccurrenceDatasetEntry> datasets = mapper.selectGeneDatasetEntries(gene, domain);
        List<DatasetRankingItem> dr = mapper.selectGeneDatasetRanking(gene, domain, DEFAULT_RANKING_LIMIT);
        List<CellContextRankingItem> cr = mapper.selectGeneCellContextRanking(gene, domain, DEFAULT_RANKING_LIMIT);

        FeatureOccurrenceResponse r = new FeatureOccurrenceResponse();
        r.setFeatureType("gene"); r.setFeatureId(gene); r.setDomain(domain); r.setAvailable(true);
        r.setTotalOccurrences(agg != null ? (int) agg.getTotal() : 0);
        r.setDatasetCount(agg != null ? (int) agg.getDatasetCount() : 0);
        r.setCellTypeCount(agg != null ? (int) agg.getCellTypeCount() : 0);
        r.setClusterCount(agg != null ? (int) agg.getClusterCount() : 0);
        r.setTopCellTypes(safe(top)); r.setDatasets(safe(datasets));
        r.setDatasetRanking(safe(dr)); r.setCellContextRanking(safe(cr));
        return r;
    }

    private FeatureOccurrenceResponse buildPeakResponse(String chrom, long start, long end, String domain) {
        FeatureOccurrenceAggregation agg = mapper.selectPeakAggregation(chrom, start, end, domain);
        List<FeatureOccurrenceTopCellType> top = mapper.selectPeakTopCellTypes(chrom, start, end, domain);
        List<FeatureOccurrenceDatasetEntry> datasets = mapper.selectPeakDatasetEntries(chrom, start, end, domain);
        List<DatasetRankingItem> dr = mapper.selectPeakDatasetRanking(chrom, start, end, domain, DEFAULT_RANKING_LIMIT);
        List<CellContextRankingItem> cr = mapper.selectPeakCellContextRanking(chrom, start, end, domain, DEFAULT_RANKING_LIMIT);

        String fid = chrom + ":" + start + "-" + end;
        FeatureOccurrenceResponse r = new FeatureOccurrenceResponse();
        r.setFeatureType("peak"); r.setFeatureId(fid); r.setDomain(domain); r.setAvailable(true);
        r.setTotalOccurrences(agg != null ? (int) agg.getTotal() : 0);
        r.setDatasetCount(agg != null ? (int) agg.getDatasetCount() : 0);
        r.setCellTypeCount(agg != null ? (int) agg.getCellTypeCount() : 0);
        r.setClusterCount(agg != null ? (int) agg.getClusterCount() : 0);
        r.setTopCellTypes(safe(top)); r.setDatasets(safe(datasets));
        r.setDatasetRanking(safe(dr)); r.setCellContextRanking(safe(cr));
        return r;
    }

    private <T> List<T> safe(List<T> list) { return list != null ? list : List.of(); }

    private String normalizeType(String type) {
        String n = trimToNull(type);
        if (n == null) throw new FeatureOccurrenceException("INVALID_PARAMETER", "type is required (gene or peak)", HttpStatus.BAD_REQUEST);
        String l = n.toLowerCase(Locale.ROOT);
        if (!"gene".equals(l) && !"peak".equals(l))
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "type must be gene or peak, got: " + n, HttpStatus.BAD_REQUEST);
        return l;
    }

    private String normalizeDomain(String value) {
        String n = trimToNull(value);
        if (n == null) return DEFAULT_DOMAIN;
        String l = n.toLowerCase(Locale.ROOT);
        if (!List.of("integration", "rna", "atac").contains(l))
            throw new FeatureOccurrenceException("INVALID_PARAMETER", "domain must be integration, rna, or atac", HttpStatus.BAD_REQUEST);
        return l;
    }

    private String trimToNull(String value) { if (value == null) return null; String t = value.trim(); return t.isEmpty() ? null : t; }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
