package com.oscar.backend.service;

import com.oscar.backend.entity.CellTypeEnrichmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisServiceImplBhTest {

    @Test
    void requestDefaultsToTheUiMinimumOverlap() {
        assertEquals(1, new CellTypeEnrichmentRequest().getMinOverlap());
    }

    @Test
    void rejectsRetiredMarkerReferencesBeforeQueryingTheDatabase() {
        CellTypeEnrichmentRequest request = new CellTypeEnrichmentRequest();
        request.setGeneSymbols(java.util.List.of("CD3D"));
        request.setMarkerReference("rna_expression");

        AnalysisServiceImpl service = new AnalysisServiceImpl(null, null);
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cellTypeEnrichment(request)
        );

        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void usesCompleteHypothesisCountIncludingOmittedUnitPValues() {
        double[] adjusted = AnalysisServiceImpl.benjaminiHochbergAdjusted(
                new double[]{0.01, 0.04},
                4
        );

        assertArrayEquals(new double[]{0.04, 0.08}, adjusted, 1e-12);
    }

    @Test
    void restoresOriginalOrderAndEnforcesMonotonicAdjustedValues() {
        double[] adjusted = AnalysisServiceImpl.benjaminiHochbergAdjusted(
                new double[]{0.50, 0.01, 0.02},
                3
        );

        assertArrayEquals(new double[]{0.50, 0.03, 0.03}, adjusted, 1e-12);
    }

    @Test
    void neverUsesFewerTestsThanObservedPValues() {
        double[] adjusted = AnalysisServiceImpl.benjaminiHochbergAdjusted(
                new double[]{0.01, 0.04},
                1
        );

        assertArrayEquals(new double[]{0.02, 0.04}, adjusted, 1e-12);
    }

    @Test
    void hypergeometricRightTailMatchesKnownExactProbability() {
        double probability = AnalysisServiceImpl.hypergeometricRightTail(
                20,
                7,
                5,
                3,
                AnalysisServiceImpl.buildLogFactorials(20)
        );

        assertEquals(3206.0 / 15504.0, probability, 1e-12);
    }

    @Test
    void hypergeometricRightTailHandlesFeasibleBoundaries() {
        double[] logFactorials = AnalysisServiceImpl.buildLogFactorials(20);

        assertEquals(1.0, AnalysisServiceImpl.hypergeometricRightTail(20, 7, 5, 0, logFactorials));
        assertEquals(0.0, AnalysisServiceImpl.hypergeometricRightTail(20, 2, 3, 3, logFactorials));
    }
}
