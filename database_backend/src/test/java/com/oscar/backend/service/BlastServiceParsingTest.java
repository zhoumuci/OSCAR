package com.oscar.backend.service;

import com.oscar.backend.entity.SequencePeak2GeneResponse.BlastHitDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlastServiceParsingTest {

    @Test
    void parsesQueryCoverageAndAppliesDeterministicScientificRanking() {
        BlastService blastService = new BlastService(null);
        List<BlastHitDto> hits = blastService.parseBlastOutput(List.of(
                "query\tchr2\t99.0\t100\t0\t0\t1\t100\t500\t599\t1e-20\t200\t100",
                "query\tchr1\t98.0\t90\t1\t0\t1\t90\t900\t811\t1e-30\t220\t90"
        ));

        assertEquals(2, hits.size());
        assertEquals("chr1", hits.get(0).getChromosome());
        assertEquals("-", hits.get(0).getStrand());
        assertEquals(90.0, hits.get(0).getQueryCoverage());
        assertEquals(1, hits.get(0).getRank());
        assertEquals("chr2", hits.get(1).getChromosome());
        assertEquals(2, hits.get(1).getRank());
    }
}
