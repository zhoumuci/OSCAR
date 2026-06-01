package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryTfSummaryResponse;

import java.util.List;

public interface RegulatoryAnnotationService {

    RegulatoryAnnotationResponse getRegulatoryAnnotations(
            String datasetId,
            String domain,
            String annotationType,
            Integer page,
            Integer pageSize,
            String targetGene,
            String peak,
            String regionType,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc
    );

    List<RegulatoryAnnotationContextOption> getRegulatoryAnnotationContextOptions(
            String datasetId,
            String domain,
            String annotationType
    );

    RegulatoryTfSummaryResponse getTfSummary(
            String datasetId,
            String domain,
            String featureType,
            String gene,
            String chrom,
            Long start,
            Long end,
            String peakId
    );
}
