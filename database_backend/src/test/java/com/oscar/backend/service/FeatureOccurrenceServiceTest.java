package com.oscar.backend.service;

import com.oscar.backend.entity.FeatureOccurrenceResponse;
import com.oscar.backend.mapper.FeatureOccurrenceMapper;
import com.oscar.backend.mapper.FeatureOccurrenceMapper.GeneOccurrenceContextRow;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeatureOccurrenceServiceTest {

    @Test
    void countsCellTypesGloballyAndClustersPerDataset() {
        FeatureOccurrenceMapper mapper = mock(FeatureOccurrenceMapper.class);
        BedtoolsQueryService bedtoolsQueryService = mock(BedtoolsQueryService.class);
        FeatureOccurrenceService service = new FeatureOccurrenceService(mapper, bedtoolsQueryService);

        when(mapper.selectGeneOccurrenceContexts("GENE1", "integration")).thenReturn(List.of(
                context("H_1", "Neuron", "C1", 2),
                context("H_1", "Astrocyte", "C2", 3),
                context("H_2", "Neuron", "C1", 4),
                context("H_2", "Neuron", "C3", 1)
        ));

        FeatureOccurrenceResponse response = service.getOccurrence(
                "gene", "GENE1", "chr1", 100L, 200L, "+", "integration", false, true
        );

        assertEquals(2, response.getDatasetCount());
        assertEquals(2, response.getCellTypeCount());
        assertEquals(4, response.getClusterCount());
        assertEquals(10, response.getTotalOccurrences());

        FeatureOccurrenceResponse.CellContextRankingItem neuron = response.getCellContextRanking().stream()
                .filter(item -> "Neuron".equals(item.getCellType()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, neuron.getDatasetCount());
        assertEquals(3, neuron.getClusterCount());

        response.getDatasetRanking().forEach(item -> assertEquals(2, item.getClusterCount()));
    }

    @Test
    void fullGeneOccurrenceReturnsEveryDatasetAndCellTypeRanking() {
        FeatureOccurrenceMapper mapper = mock(FeatureOccurrenceMapper.class);
        BedtoolsQueryService bedtoolsQueryService = mock(BedtoolsQueryService.class);
        FeatureOccurrenceService service = new FeatureOccurrenceService(mapper, bedtoolsQueryService);
        List<GeneOccurrenceContextRow> contexts = new ArrayList<>();
        for (int index = 0; index < 966; index++) {
            contexts.add(context(
                    "H_" + index,
                    "CellType_" + (index % 133),
                    "C" + index,
                    1
            ));
        }
        when(mapper.selectGeneOccurrenceContexts("SAMD3", "integration")).thenReturn(contexts);

        FeatureOccurrenceResponse limited = service.getOccurrence(
                "gene", "SAMD3", "chr1", 100L, 200L, "+", "integration", false, false
        );
        FeatureOccurrenceResponse full = service.getOccurrence(
                "gene", "SAMD3", "chr1", 100L, 200L, "+", "integration", false, true
        );

        assertEquals(10, limited.getDatasetRanking().size());
        assertEquals(10, limited.getCellContextRanking().size());
        assertEquals(100, limited.getDatasets().size());
        assertEquals(966, full.getDatasetRanking().size());
        assertEquals(133, full.getCellContextRanking().size());
        assertEquals(966, full.getDatasets().size());
        assertEquals(966, full.getDatasetCount());
        assertEquals(133, full.getCellTypeCount());
    }

    @Test
    void occurrenceCacheSeparatesLimitedAndFullResponses() throws Exception {
        Cacheable cacheable = FeatureOccurrenceService.class.getMethod(
                "getOccurrence",
                String.class,
                String.class,
                String.class,
                Long.class,
                Long.class,
                String.class,
                String.class,
                boolean.class,
                boolean.class
        ).getAnnotation(Cacheable.class);

        assertTrue(cacheable.key().contains("#contextOnly"));
        assertTrue(cacheable.key().contains("#full"));
    }

    private static GeneOccurrenceContextRow context(
            String datasetId,
            String cellType,
            String cluster,
            long recordCount
    ) {
        GeneOccurrenceContextRow row = new GeneOccurrenceContextRow();
        row.setDatasetId(datasetId);
        row.setSampleName(datasetId);
        row.setCellType(cellType);
        row.setCluster(cluster);
        row.setRecordCount(recordCount);
        return row;
    }
}
