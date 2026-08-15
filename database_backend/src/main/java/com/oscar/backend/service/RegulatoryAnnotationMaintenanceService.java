package com.oscar.backend.service;

import com.oscar.backend.entity.GeneMarkerSummaryRefreshResponse;
import com.oscar.backend.entity.LinkedRegionRefreshResponse;

public interface RegulatoryAnnotationMaintenanceService {

    LinkedRegionRefreshResponse refreshMarkerLinkedRegions(String datasetId, String domain);

    GeneMarkerSummaryRefreshResponse refreshGeneMarkerSummary(String datasetId);
}
