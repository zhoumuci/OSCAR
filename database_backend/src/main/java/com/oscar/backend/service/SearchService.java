package com.oscar.backend.service;

import com.oscar.backend.entity.GeneSearchRequest;
import com.oscar.backend.entity.GeneSearchResponse;
import com.oscar.backend.entity.PeakSearchRequest;
import com.oscar.backend.entity.PeakSearchResponse;
import com.oscar.backend.mapper.SearchMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final SearchMapper mapper;

    public SearchService(SearchMapper mapper) { this.mapper = mapper; }

    public GeneSearchResponse searchByGene(GeneSearchRequest req) {
        List<String> genes = req.getGenes().stream()
                .map(String::trim).map(String::toUpperCase)
                .filter(g -> !g.isEmpty()).distinct().sorted().toList();

        if (genes.isEmpty()) return emptyResponse();

        boolean intersection = "intersection".equals(req.getMatchMode());
        long t0 = System.currentTimeMillis();
        String domain = normalizeFilter(req.getDomain());
        String signalType = normalizeSignalType(req.getSignalType());
        String tissue = normalizeFilter(req.getTissue());
        int resultSize = Math.max(0, req.getResultSize());
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "sampleName";

        List<Map<String, Object>> candidates = new ArrayList<>(mapper.findGeneSampleStats(
                genes, genes.size(), intersection, domain, signalType, tissue
        ));
        if (candidates.isEmpty()) return emptyResponse();
        long candidateMs = System.currentTimeMillis() - t0;

        if ("geneCount".equals(sortBy)) {
            candidates.sort(Comparator
                    .comparingInt((Map<String, Object> row) -> intValue(value(row, "gene_count"))).reversed()
                    .thenComparing(SearchService::sampleName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(SearchService::datasetId));
        } else {
            candidates.sort(Comparator
                    .comparing(SearchService::sampleName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(SearchService::datasetId));
        }

        List<Map<String, Object>> displayedCandidates = candidates;
        if (resultSize > 0 && candidates.size() > resultSize) {
            displayedCandidates = new ArrayList<>(candidates.subList(0, resultSize));
        }

        // Build response
        List<GeneSearchResponse.SampleItem> items = new ArrayList<>();
        for (Map<String, Object> s : displayedCandidates) {
            String id = datasetId(s);
            GeneSearchResponse.SampleItem item = new GeneSearchResponse.SampleItem();
            item.setSampleId(id);
            item.setSampleName(str(s.get("sample_name")));
            item.setTissue(str(s.get("tissue")));
            item.setCellContext(str(s.get("sample_type")));
            item.setCellCount(intValue(s.get("cell_count")));
            item.setPlatform(str(s.get("platform")));
            item.setSourceId(str(s.get("source_id")));
            item.setDisease(str(s.get("disease")));
            item.setSampleSource(str(s.get("sample_source")));
            item.setMatchedGenes(intValue(s.get("gene_count")));
            items.add(item);
        }

        long totalEvidence = candidates.stream()
                .mapToLong(row -> longValue(row.get("total_evidence")))
                .sum();
        GeneSearchResponse.Summary sum = new GeneSearchResponse.Summary();
        sum.setMatchedSamples(candidates.size());
        sum.setMarkerGeneEvidence(totalEvidence);

        GeneSearchResponse resp = new GeneSearchResponse();
        resp.setSummary(sum);
        resp.setSamples(items);

        log.info("Gene search: {} genes -> {} matched / {} returned in {} ms "
                        + "(candidates={} ms)",
                genes.size(), candidates.size(), items.size(), System.currentTimeMillis() - t0,
                candidateMs);
        return resp;
    }

    private GeneSearchResponse emptyResponse() {
        GeneSearchResponse r = new GeneSearchResponse();
        GeneSearchResponse.Summary s = new GeneSearchResponse.Summary();
        r.setSummary(s); r.setSamples(List.of());
        return r;
    }

    public PeakSearchResponse searchByPeak(PeakSearchRequest req) {
        long t0 = System.nanoTime();
        String datasetId = normalizeFilter(req.getDatasetId());
        if (datasetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datasetId is required");
        }
        List<PeakSearchRequest.Region> regions = normalizePeakRegions(req.getRegions());
        if (regions.isEmpty()) return emptyPeakResponse(0);

        boolean matchAll = "all".equalsIgnoreCase(req.getMatchMode());
        String requestedDomain = normalizeFilter(req.getDomain());
        String domain = requestedDomain != null ? requestedDomain : "integration";

        CompletableFuture<TimedRows> markerFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            List<Map<String, Object>> rows = regions.size() == 1
                    ? mapper.findSamplesBySinglePeakRegion(regions.get(0), domain, datasetId)
                    : mapper.findSamplesByPeaks(regions, regions.size(), matchAll, domain, datasetId);
            return new TimedRows(rows, (System.nanoTime() - start) / 1_000_000L);
        });
        CompletableFuture<TimedRows> linkedGeneFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            List<Map<String, Object>> rows = regions.size() == 1
                    ? mapper.findLinkedGeneCountsForSinglePeakRegion(regions.get(0), domain, datasetId)
                    : mapper.findLinkedGeneCountsForPeaks(regions, domain, datasetId);
            return new TimedRows(rows, (System.nanoTime() - start) / 1_000_000L);
        });

        TimedRows markerResult = markerFuture.join();
        TimedRows linkedGeneResult = linkedGeneFuture.join();
        List<Map<String, Object>> hits = markerResult.rows();
        if (hits.isEmpty()) return emptyPeakResponse(regions.size());

        Map<String, Long> linkedGeneCounts = new HashMap<>();
        for (Map<String, Object> row : linkedGeneResult.rows()) {
            linkedGeneCounts.put(datasetId(row), longValue(row.get("gene_count")));
        }

        List<PeakSearchResponse.SampleItem> items = new ArrayList<>();
        long totalPeaks = 0L;
        long totalGenes = 0L;
        Set<Integer> matchedRegionIds = new HashSet<>();
        for (Map<String, Object> row : hits) {
            addRegionIds(matchedRegionIds, row.get("matched_region_ids"));
            long peakCount = longValue(row.get("peak_count"));
            long geneCount = linkedGeneCounts.getOrDefault(datasetId(row), 0L);
            PeakSearchResponse.SampleItem item = new PeakSearchResponse.SampleItem();
            item.setSampleId(datasetId(row));
            item.setSampleName(str(row.get("sample_name")));
            item.setTissue(str(row.get("tissue")));
            item.setCellContext(str(row.get("sample_type")));
            item.setCellCount(intValue(row.get("cell_count")));
            item.setPlatform(str(row.get("platform")));
            item.setSourceId(str(row.get("source_id")));
            item.setDisease(str(row.get("disease")));
            item.setSampleSource(str(row.get("sample_source")));
            item.setMatchedRegions(intValue(row.get("matched_regions")));
            item.setOverlappingPeaks(peakCount);
            item.setLinkedGenes(geneCount);
            item.setHasAtac(true);
            item.setHasP2g(geneCount > 0);
            totalPeaks += peakCount;
            totalGenes += geneCount;
            items.add(item);
        }

        PeakSearchResponse.Summary summary = new PeakSearchResponse.Summary();
        summary.setInputRegions(regions.size());
        summary.setMatchedSamples(items.size());
        summary.setMatchedInputRegions(matchedRegionIds.size());
        summary.setOverlappingPeaks(totalPeaks);
        summary.setLinkedGenes(totalGenes);

        PeakSearchResponse response = new PeakSearchResponse();
        response.setSummary(summary);
        response.setSamples(items);
        log.info("Peak search: datasetId={}, {} regions ({}) -> {} samples, {} peaks, {} genes in {} ms "
                        + "(markers={} ms, linkedGenes={} ms)",
                datasetId, regions.size(), matchAll ? "all" : "any", items.size(), totalPeaks, totalGenes,
                (System.nanoTime() - t0) / 1_000_000L,
                markerResult.elapsedMs(), linkedGeneResult.elapsedMs());
        return response;
    }

    private static void addRegionIds(Set<Integer> target, Object value) {
        if (value == null) return;
        for (String token : value.toString().split(",")) {
            try {
                target.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed aggregate tokens; query-generated indices are numeric.
            }
        }
    }

    private record TimedRows(List<Map<String, Object>> rows, long elapsedMs) {}

    private static List<PeakSearchRequest.Region> normalizePeakRegions(List<PeakSearchRequest.Region> input) {
        if (input == null || input.isEmpty()) return List.of();
        Map<String, PeakSearchRequest.Region> unique = new LinkedHashMap<>();
        for (PeakSearchRequest.Region source : input) {
            if (source == null) continue;
            String chrom = normalizeFilter(source.getChrom());
            if (chrom == null) continue;
            if (chrom.regionMatches(true, 0, "chr", 0, 3)) {
                chrom = "chr" + chrom.substring(3);
            }
            long start = Math.min(source.getStart(), source.getEnd());
            long end = Math.max(source.getStart(), source.getEnd());
            if (start < 0 || start >= end) continue;
            String key = chrom.toLowerCase(Locale.ROOT) + ':' + start + '-' + end;
            if (unique.containsKey(key)) continue;
            PeakSearchRequest.Region normalized = new PeakSearchRequest.Region();
            normalized.setChrom(chrom);
            normalized.setStart(start);
            normalized.setEnd(end);
            unique.put(key, normalized);
        }
        return new ArrayList<>(unique.values());
    }

    private static PeakSearchResponse emptyPeakResponse(int inputRegions) {
        PeakSearchResponse.Summary summary = new PeakSearchResponse.Summary();
        summary.setInputRegions(inputRegions);
        PeakSearchResponse response = new PeakSearchResponse();
        response.setSummary(summary);
        response.setSamples(List.of());
        return response;
    }

    public GeneSearchResponse searchByTissue(List<String> tissues) {
        List<String> normalized = tissues == null ? List.of() : tissues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
        if (normalized.isEmpty()) return emptyResponse();

        List<Map<String, Object>> hits = mapper.findSamplesByTissue(normalized);
        if (hits.isEmpty()) return emptyResponse();

        List<GeneSearchResponse.SampleItem> items = new ArrayList<>();
        for (Map<String, Object> s : hits) {
            GeneSearchResponse.SampleItem item = new GeneSearchResponse.SampleItem();
            item.setSampleId(str(value(s, "datasetId", "dataset_id")));
            item.setSampleName(str(value(s, "sampleName", "sample_name")));
            item.setTissue(str(value(s, "tissue")));
            item.setCellContext(str(value(s, "cellContext", "sample_type")));
            item.setCellCount(intValue(value(s, "cellCount", "cell_count")));
            item.setPlatform(str(value(s, "platform")));
            item.setSourceId(str(value(s, "sourceId", "source_id")));
            item.setDisease(str(value(s, "disease")));
            item.setSampleSource(str(value(s, "sampleSource", "sample_source")));
            item.setMatchedGenes(0);
            item.setLinkedPeaks(0);
            item.setHasAtac(false);
            item.setHasRna(true);
            items.add(item);
        }

        GeneSearchResponse.Summary sum = new GeneSearchResponse.Summary();
        sum.setMatchedSamples(items.size());
        GeneSearchResponse resp = new GeneSearchResponse();
        resp.setSummary(sum);
        resp.setSamples(items);
        return resp;
    }

    public GeneSearchResponse searchByCellType(String cellType) {
        String normalized = cellType == null ? "" : cellType.trim();
        if (normalized.isEmpty()) return emptyResponse();
        List<Map<String, Object>> hits = mapper.findSamplesByCellType(normalized);
        if (hits.isEmpty()) return emptyResponse();

        List<GeneSearchResponse.SampleItem> items = new ArrayList<>();
        for (Map<String, Object> s : hits) {
            GeneSearchResponse.SampleItem item = new GeneSearchResponse.SampleItem();
            item.setSampleId(str(value(s, "dataset_id")));
            item.setSampleName(str(value(s, "sample_name")));
            item.setTissue(str(value(s, "tissue")));
            item.setCellContext(str(value(s, "cellContext", "sample_type")));
            item.setCellCount(intValue(value(s, "cell_count")));
            item.setPlatform(str(value(s, "platform")));
            item.setSourceId(str(value(s, "source_id")));
            item.setDisease(str(value(s, "disease")));
            item.setSampleSource(str(value(s, "sample_source")));
            item.setMatchedGenes(intValue(value(s, "cluster_count")));
            item.setMatchedCells(longValue(value(s, "matched_cell_count")));
            item.setHasAtac(false);
            item.setHasRna(true);
            items.add(item);
        }

        GeneSearchResponse.Summary sum = new GeneSearchResponse.Summary();
        sum.setMatchedSamples(items.size());
        GeneSearchResponse resp = new GeneSearchResponse();
        resp.setSummary(sum);
        resp.setSamples(items);
        return resp;
    }

    private GeneSearchResponse buildResponse(List<String> dsIds, List<Map<String, Object>> hits, String mode) {
        String tissue = null;
        Map<String, Map<String, Object>> sampleMap = new LinkedHashMap<>();
        for (Map<String, Object> s : mapper.getSampleMeta(dsIds, tissue)) {
            sampleMap.put((String) s.get("dataset_id"), s);
        }
        List<GeneSearchResponse.SampleItem> items = new ArrayList<>();
        for (String id : dsIds) {
            Map<String, Object> s = sampleMap.get(id);
            if (s == null) continue;
            GeneSearchResponse.SampleItem item = new GeneSearchResponse.SampleItem();
            item.setSampleId(id);
            item.setSampleName(str(s.get("sample_name")));
            item.setTissue(str(s.get("tissue")));
            item.setCellContext(str(s.get("sample_type")));
            item.setMatchedGenes(0);
            item.setLinkedPeaks(0);
            item.setHasAtac(false); item.setHasRna(true);
            item.setCellCount(intValue(s.get("cell_count")));
            items.add(item);
        }
        GeneSearchResponse.Summary sum = new GeneSearchResponse.Summary();
        sum.setMatchedSamples(items.size());
        GeneSearchResponse resp = new GeneSearchResponse();
        resp.setSummary(sum); resp.setSamples(items);
        return resp;
    }

    public List<String> listCellTypes() { return mapper.listDistinctCellTypes(); }
    public List<Map<String, Object>> tissueCounts() { return mapper.tissueCounts(); }
    public List<Map<String, Object>> cellTypeCounts() { return mapper.cellTypeCounts(); }

    private static List<String> datasetIds(List<Map<String, Object>> rows) {
        return rows.stream().map(SearchService::datasetId).toList();
    }

    private static String datasetId(Map<String, Object> row) {
        return str(value(row, "dataset_id", "datasetId"));
    }

    private static String sampleName(Map<String, Object> row) {
        return str(value(row, "sample_name", "sampleName"));
    }

    private static String normalizeFilter(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeSignalType(String value) {
        String v = normalizeFilter(value);
        if (v == null) return null;
        if ("gene_score".equals(v)) return "gene_score";
        if ("gene_exp".equals(v) || "gene_expression".equals(v)) return "gene_expression";
        return null;
    }

    private static Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) return row.get(key);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
            }
        }
        return null;
    }

    private static int intValue(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v == null) return 0;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static long longValue(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v == null) return 0L;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String str(Object v) { return v == null ? "" : v.toString(); }
}
