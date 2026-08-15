package com.oscar.backend.service;

import com.oscar.backend.entity.PeakGeneContextRequest;
import com.oscar.backend.entity.PeakGeneContextResponse;
import com.oscar.backend.mapper.PeakGeneContextMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PeakGeneContextServiceTest {

    @Test
    void generalMarkerModeCollapsesContextsAndKeepsEveryMarkerType() {
        PeakGeneContextMapper mapper = mock(PeakGeneContextMapper.class);
        PeakGeneContextBedtoolsService bedtools = mock(PeakGeneContextBedtoolsService.class);
        PeakGeneContextService service = new PeakGeneContextService(mapper, bedtools);
        List<Map<String, Object>> candidates = new ArrayList<>(List.of(
                markerCandidate(1L, "gene_exp", "Neuron", "Neuron|C1"),
                markerCandidate(2L, "gene_score", "Astrocyte", "Astrocyte|C2")
        ));

        when(mapper.selectP2gDatasets("Brain")).thenReturn(List.of(Map.of("dataset_id", "H_1")));
        when(mapper.selectMarkerCandidates(List.of("LYN"), "Brain", null, false)).thenReturn(candidates);
        when(bedtools.intersect(anyList(), eq(candidates), eq(1))).thenReturn(candidates);

        PeakGeneContextResponse response = service.analyze(request("p2g_markers"));

        assertEquals(1, response.getSummary().getTotalPairs());
        assertEquals(1, response.getPairs().size());
        PeakGeneContextResponse.PairDetail pair = response.getPairs().get(0);
        assertTrue(pair.isHasMarkerPeak());
        assertTrue(pair.isHasMarkerGene());
        assertEquals(List.of("gene_expression", "gene_score"), pair.getGeneMarkerTypes());
        assertNull(pair.getSignalType());
        assertNull(pair.getCellType());
        assertNull(pair.getContextLabel());
        assertNull(pair.getClusterLabel());
    }

    @Test
    void generalRawModeAnnotatesBothEndpointsAfterCollapsingLinks() {
        PeakGeneContextMapper mapper = mock(PeakGeneContextMapper.class);
        PeakGeneContextBedtoolsService bedtools = mock(PeakGeneContextBedtoolsService.class);
        PeakGeneContextService service = new PeakGeneContextService(mapper, bedtools);
        List<Map<String, Object>> candidates = new ArrayList<>(List.of(rawCandidate()));

        when(mapper.selectP2gDatasets("Brain")).thenReturn(List.of(Map.of("dataset_id", "H_1")));
        when(mapper.selectRawCandidates(List.of("LYN"), "Brain", null)).thenReturn(candidates);
        when(bedtools.intersect(anyList(), eq(candidates), eq(1))).thenReturn(candidates);
        when(mapper.selectGeneMarkerSignals(List.of("H_1"), List.of("LYN"))).thenReturn(List.of(
                Map.of("dataset_id", "H_1", "gene_symbol", "LYN", "signal_type", "gene_exp"),
                Map.of("dataset_id", "H_1", "gene_symbol", "LYN", "signal_type", "gene_score")
        ));
        when(mapper.selectMarkerPeakKeys(List.of("H_1"), List.of("chr8:100-200")))
                .thenReturn(List.of(Map.of("dataset_id", "H_1", "peak_name", "chr8:100-200")));

        PeakGeneContextResponse response = service.analyze(request("p2g_only"));

        PeakGeneContextResponse.PairDetail pair = response.getPairs().get(0);
        assertTrue(pair.isHasMarkerPeak());
        assertTrue(pair.isHasMarkerGene());
        assertEquals(List.of("gene_expression", "gene_score"), pair.getGeneMarkerTypes());
    }

    @Test
    void cellTypeModeRanksReturnedRowsByMatchedRecordsWithoutUsingGlobalLinkMetrics() {
        PeakGeneContextMapper mapper = mock(PeakGeneContextMapper.class);
        PeakGeneContextBedtoolsService bedtools = mock(PeakGeneContextBedtoolsService.class);
        PeakGeneContextService service = new PeakGeneContextService(mapper, bedtools);

        Map<String, Object> neuronFirst = markerCandidate(1L, "gene_exp", "Neuron", "Neuron|C1");
        neuronFirst.put("link_score", 0.10D);
        neuronFirst.put("link_fdr", 0.90D);
        Map<String, Object> neuronSecond = markerCandidate(2L, "gene_score", "Neuron", "Neuron|C2");
        neuronSecond.put("link_score", 0.10D);
        neuronSecond.put("link_fdr", 0.90D);
        Map<String, Object> astrocyte = markerCandidate(3L, "gene_exp", "Astrocyte", "Astrocyte|C3");
        astrocyte.put("link_score", 0.99D);
        astrocyte.put("link_fdr", 0.001D);
        List<Map<String, Object>> candidates = new ArrayList<>(List.of(astrocyte, neuronFirst, neuronSecond));

        when(mapper.selectP2gDatasets("Brain")).thenReturn(List.of(Map.of("dataset_id", "H_1")));
        when(mapper.selectMarkerCandidates(List.of("LYN"), "Brain", null, true)).thenReturn(candidates);
        when(bedtools.intersect(anyList(), eq(candidates), eq(1))).thenReturn(candidates);

        PeakGeneContextRequest request = request("p2g_markers");
        request.setResultType("cell_type");
        request.getAdvanced().setMaxReturnedLinks(1);
        PeakGeneContextResponse response = service.analyze(request);

        assertEquals("Neuron", response.getCellTypeResults().get(0).getCellType());
        assertEquals(2, response.getCellTypeResults().get(0).getEvidenceCount());
        assertEquals("Neuron", response.getPairs().get(0).getCellType());
        assertNull(response.getPairs().get(0).getLinkScore());
        assertNull(response.getPairs().get(0).getLinkFdr());
        assertTrue(response.getNetworkData().getEdges().stream().allMatch(edge -> edge.getWeight() == null));
    }

    @Test
    void missingReturnLimitReturnsEveryMatchedRow() {
        PeakGeneContextMapper mapper = mock(PeakGeneContextMapper.class);
        PeakGeneContextBedtoolsService bedtools = mock(PeakGeneContextBedtoolsService.class);
        PeakGeneContextService service = new PeakGeneContextService(mapper, bedtools);
        List<Map<String, Object>> candidates = new ArrayList<>(List.of(
                markerCandidate(1L, "gene_exp", "Neuron", "Neuron|C1"),
                markerCandidate(2L, "gene_score", "Neuron", "Neuron|C2"),
                markerCandidate(3L, "gene_exp", "Astrocyte", "Astrocyte|C3")
        ));

        when(mapper.selectP2gDatasets("Brain")).thenReturn(List.of(Map.of("dataset_id", "H_1")));
        when(mapper.selectMarkerCandidates(List.of("LYN"), "Brain", null, true)).thenReturn(candidates);
        when(bedtools.intersect(anyList(), eq(candidates), eq(1))).thenReturn(candidates);

        PeakGeneContextRequest request = request("p2g_markers");
        request.setResultType("cell_type");
        PeakGeneContextResponse response = service.analyze(request);

        assertEquals(3, response.getSummary().getTotalPairs());
        assertEquals(3, response.getPairs().size());
    }

    @Test
    void networkKeepsTopFiftyUniquePeaksPerGeneWithoutLimitingReturnedPairs() {
        PeakGeneContextMapper mapper = mock(PeakGeneContextMapper.class);
        PeakGeneContextBedtoolsService bedtools = mock(PeakGeneContextBedtoolsService.class);
        PeakGeneContextService service = new PeakGeneContextService(mapper, bedtools);
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            Map<String, Object> candidate = markerCandidate(index + 1L, "gene_exp", "Neuron", "Neuron|C1");
            candidate.put("peak_name", "chr8:" + (100 + index) + "-" + (200 + index));
            candidate.put("peak_start", 100L + index);
            candidate.put("peak_end", 200L + index);
            candidate.put("link_score", index / 100.0D);
            candidates.add(candidate);
        }

        when(mapper.selectP2gDatasets("Brain")).thenReturn(List.of(Map.of("dataset_id", "H_1")));
        when(mapper.selectMarkerCandidates(List.of("LYN"), "Brain", null, false)).thenReturn(candidates);
        when(bedtools.intersect(anyList(), eq(candidates), eq(1))).thenReturn(candidates);

        PeakGeneContextResponse response = service.analyze(request("p2g_markers"));

        assertEquals(51, response.getPairs().size());
        assertEquals(50, response.getNetworkData().getPeakLimitPerGene());
        assertEquals(50, response.getNetworkData().getNodes().stream()
                .filter(node -> "peak".equals(node.getCategory()))
                .count());
        assertTrue(response.getNetworkData().getNodes().stream()
                .noneMatch(node -> "chr8:100-200".equals(node.getName())));
        assertEquals(50, response.getNetworkData().getEdges().size());
    }

    private static PeakGeneContextRequest request(String referenceMode) {
        PeakGeneContextRequest.PeakInput peak = new PeakGeneContextRequest.PeakInput();
        peak.setChrom("chr8");
        peak.setStart(100);
        peak.setEnd(200);

        PeakGeneContextRequest.Advanced advanced = new PeakGeneContextRequest.Advanced();
        advanced.setMinOverlapBp(1);

        PeakGeneContextRequest request = new PeakGeneContextRequest();
        request.setPeaks(List.of(peak));
        request.setGenes(List.of("LYN"));
        request.setTissue("Brain");
        request.setReferenceMode(referenceMode);
        request.setResultType("general");
        request.setAdvanced(advanced);
        return request;
    }

    private static Map<String, Object> rawCandidate() {
        Map<String, Object> row = new HashMap<>();
        row.put("evidence_id", 1L);
        row.put("peak_name", "chr8:100-200");
        row.put("chromosome", "chr8");
        row.put("peak_start", 100L);
        row.put("peak_end", 200L);
        row.put("gene_name", "LYN");
        row.put("dataset_id", "H_1");
        row.put("link_score", 0.9D);
        row.put("link_fdr", 0.001D);
        return row;
    }

    private static Map<String, Object> markerCandidate(
            long evidenceId,
            String signalType,
            String cellType,
            String contextLabel
    ) {
        Map<String, Object> row = rawCandidate();
        row.put("evidence_id", evidenceId);
        row.put("signal_type", signalType);
        row.put("cell_type", cellType);
        row.put("context_label", contextLabel);
        row.put("cluster_label", contextLabel.substring(contextLabel.indexOf('|') + 1));
        row.put("has_marker_peak", 1);
        return row;
    }
}
