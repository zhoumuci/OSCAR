package com.oscar.backend.service;

import com.oscar.backend.entity.SearchResultCellTypeCompositionResponse;
import com.oscar.backend.entity.SearchResultOverviewResponse;
import com.oscar.backend.entity.SearchResultQcViolinResponse;
import com.oscar.backend.entity.SearchResultUmapResponse;

public interface SearchResultService {

    SearchResultOverviewResponse getOverview(String datasetId);

    SearchResultCellTypeCompositionResponse getCellTypeComposition(String datasetId, String domain, String groupBy);

    SearchResultQcViolinResponse getQcViolin(String datasetId, String domain, String groupBy, String metrics);

    SearchResultUmapResponse getUmap(String datasetId, String domain, String embedding, String colorBy, Integer maxPoints);
}
