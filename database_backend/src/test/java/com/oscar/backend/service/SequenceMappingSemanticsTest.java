package com.oscar.backend.service;

import com.oscar.backend.entity.SequencePeak2GeneResponse.BlastHitDto;
import com.oscar.backend.entity.SequencePeak2GeneRequest;
import com.oscar.backend.entity.SequencePeak2GeneResponse;
import com.oscar.backend.mapper.AnalysisMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SequenceMappingSemanticsTest {

    @Test
    void convertsBlastInclusiveCoordinatesToBedHalfOpenCoordinates() {
        BlastHitDto hit = hit("chr1", 100, 200, "+", 100, 250);

        assertArrayEquals(new long[]{89, 210}, AnalysisServiceImpl.toBedRegion(hit, 10));
    }

    @Test
    void collapsesOverlappingHspsAtOnePhysicalLocusButKeepsSeparateLoci() {
        BlastHitDto strongest = hit("chr2", 100, 200, "+", 100, 300);
        BlastHitDto overlappingFragment = hit("chr2", 150, 220, "-", 90, 150);
        BlastHitDto separate = hit("chr2", 500, 600, "+", 100, 290);

        List<BlastHitDto> candidates = AnalysisServiceImpl.distinctCandidateLoci(
                List.of(overlappingFragment, separate, strongest)
        );

        assertEquals(2, candidates.size());
        assertEquals(100, candidates.get(0).getStart());
        assertEquals(500, candidates.get(1).getStart());
    }

    @Test
    void marksNearEquivalentQualifiedLociAsAmbiguous() {
        BlastHitDto first = hit("chr1", 100, 200, "+", 100, 300);
        BlastHitDto second = hit("chr3", 500, 600, "+", 100, 291);

        List<BlastHitDto> candidates = AnalysisServiceImpl.distinctCandidateLoci(
                List.of(first, second)
        );
        AnalysisServiceImpl.MappingDecision decision = AnalysisServiceImpl.classifyMapping(candidates);

        assertEquals("AMBIGUOUS", decision.status());
        assertEquals(2, decision.qualifiedCount());
        assertEquals(1, decision.nearEquivalentCount());
        assertTrue(candidates.get(1).isNearEquivalent());
    }

    @Test
    void lowCoverageAlignmentsStillClassifyAsUniqueWhenSingleCandidate() {
        BlastHitDto partial = hit("chr4", 1000, 1050, "+", 50, 180);

        List<BlastHitDto> candidates = AnalysisServiceImpl.distinctCandidateLoci(List.of(partial));
        AnalysisServiceImpl.MappingDecision decision = AnalysisServiceImpl.classifyMapping(candidates);

        assertEquals("UNIQUE", decision.status());
        assertFalse(candidates.get(0).isNearEquivalent());
    }

    @Test
    void autoRetriesWithBlastnOnlyWhenMegablastReturnsNoHit() throws IOException {
        AnalysisMapper mapper = mock(AnalysisMapper.class);
        BlastService blast = mock(BlastService.class);
        when(blast.cleanSequence(anyString())).thenReturn("A".repeat(100));
        when(blast.runBlast(anyString(), eq("megablast"), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of());
        when(blast.runBlast(anyString(), eq("blastn"), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of(hit("chr2", 500, 599, "+", 100, 300)));
        when(mapper.selectP2gLinksByRegion(eq("chr2"), eq(499L), eq(599L), isNull(), eq(0)))
                .thenReturn(List.of());
        when(mapper.selectMarkerPeaksByRegion(eq("chr2"), eq(499L), eq(599L), isNull(), eq(0)))
                .thenReturn(List.of());

        SequencePeak2GeneRequest request = new SequencePeak2GeneRequest();
        request.setSequence("A".repeat(100));
        List<String> stages = new ArrayList<>();
        SequencePeak2GeneResponse response = new AnalysisServiceImpl(mapper, blast)
                .sequencePeak2Gene(request, (progress, stage, message) -> stages.add(stage));

        assertEquals("chr2", response.getBlastHits().get(0).getChromosome());
        assertTrue(stages.contains("BLAST_FALLBACK"));
        verify(blast).runBlast(anyString(), eq("megablast"), anyInt(), anyInt(), anyDouble());
        verify(blast).runBlast(anyString(), eq("blastn"), anyInt(), anyInt(), anyDouble());
    }

    @Test
    void ambiguousLociNeverPoolRegulatoryEvidence() throws IOException {
        AnalysisMapper mapper = mock(AnalysisMapper.class);
        BlastService blast = mock(BlastService.class);
        when(blast.cleanSequence(anyString())).thenReturn("A".repeat(100));
        when(blast.runBlast(anyString(), anyString(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of(
                        hit("chr1", 100, 200, "+", 100, 300),
                        hit("chr3", 500, 600, "+", 100, 300)
                ));
        when(mapper.selectP2gLinksByRegion(eq("chr1"), eq(99L), eq(200L), isNull(), eq(0)))
                .thenReturn(List.of());
        when(mapper.selectMarkerPeaksByRegion(eq("chr1"), eq(99L), eq(200L), isNull(), eq(0)))
                .thenReturn(List.of());

        SequencePeak2GeneRequest request = new SequencePeak2GeneRequest();
        request.setSequence("A".repeat(100));
        SequencePeak2GeneResponse response = new AnalysisServiceImpl(mapper, blast)
                .sequencePeak2Gene(request);

        assertEquals("AMBIGUOUS", response.getMappingStatus());
        assertEquals("hit-1", response.getEvidenceHitId());
        verify(mapper).selectP2gLinksByRegion("chr1", 99L, 200L, null, 0);
        verify(mapper).selectMarkerPeaksByRegion("chr1", 99L, 200L, null, 0);
        verify(mapper, never()).selectP2gLinksByRegion(eq("chr3"), anyLong(), anyLong(), isNull(), anyInt());
        verify(mapper, never()).selectMarkerPeaksByRegion(eq("chr3"), anyLong(), anyLong(), isNull(), anyInt());
    }

    @Test
    void markerPeakResultsAggregateAllP2gGenesWithoutReturningTheP2gTabPayload() throws IOException {
        AnalysisMapper mapper = mock(AnalysisMapper.class);
        BlastService blast = mock(BlastService.class);
        when(blast.cleanSequence(anyString())).thenReturn("A".repeat(100));
        when(blast.runBlast(anyString(), anyString(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of(hit("chr1", 100, 200, "+", 100, 300)));
        when(mapper.selectMarkerPeaksByRegion("chr1", 99L, 200L, null, 0))
                .thenReturn(List.of(
                        markerP2gRow(7L, "GENE1", 0.8),
                        markerP2gRow(7L, "GENE2", 0.7)
                ));

        SequencePeak2GeneRequest request = new SequencePeak2GeneRequest();
        request.setSequence("A".repeat(100));
        request.setResultContent("marker_peaks");
        SequencePeak2GeneResponse response = new AnalysisServiceImpl(mapper, blast)
                .sequencePeak2Gene(request);

        assertTrue(response.getPeakGeneLinks().isEmpty());
        assertEquals(1, response.getMarkerPeaks().size());
        assertEquals(2, response.getMarkerPeaks().get(0).getPeakGeneLinks().size());
        assertEquals(List.of("GENE1", "GENE2"), response.getMarkerPeaks().get(0).getPeakGeneLinks()
                .stream().map(SequencePeak2GeneResponse.PeakGeneLinkDto::getGeneName).toList());
        assertEquals(2, response.getSummary().getLinkedGeneCount());
        verify(mapper, never()).selectP2gLinksByRegion(anyString(), anyLong(), anyLong(), isNull(), anyInt());
    }

    private static BlastHitDto hit(
            String chromosome,
            long start,
            long end,
            String strand,
            double queryCoverage,
            double bitScore
    ) {
        BlastHitDto hit = new BlastHitDto();
        hit.setChromosome(chromosome);
        hit.setStart(start);
        hit.setEnd(end);
        hit.setStrand(strand);
        hit.setQueryCoverage(queryCoverage);
        hit.setIdentity(99.0);
        hit.setEvalue("1e-30");
        hit.setBitScore(bitScore);
        return hit;
    }

    private static Map<String, Object> markerP2gRow(long markerPeakId, String geneName, double linkScore) {
        Map<String, Object> row = new HashMap<>();
        row.put("marker_peak_id", markerPeakId);
        row.put("dataset_id", "H_000001");
        row.put("domain", "integration");
        row.put("cluster_source", "WNN");
        row.put("group_name", "T cells");
        row.put("peak_name", "chr1:120-180");
        row.put("chromosome", "chr1");
        row.put("peak_start", 120L);
        row.put("peak_end", 180L);
        row.put("log2fc", 1.5);
        row.put("fdr", 0.01);
        row.put("mean_diff", 0.4);
        row.put("source_file", "marker.tsv");
        row.put("p2g_peak_name", "chr1:120-180");
        row.put("p2g_chromosome", "chr1");
        row.put("p2g_peak_start", 120L);
        row.put("p2g_peak_end", 180L);
        row.put("p2g_gene_name", geneName);
        row.put("p2g_correlation", linkScore);
        row.put("p2g_fdr", 0.02);
        row.put("p2g_link_score", linkScore);
        row.put("p2g_source_file", "p2g.tsv");
        return row;
    }
}
