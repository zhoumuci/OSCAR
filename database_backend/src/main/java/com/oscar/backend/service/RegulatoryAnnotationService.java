package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryAnnotationResponse;
import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryTfSummaryResponse;

import java.io.IOException;
import java.io.OutputStream;
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
            Double minLog2fc,
            Double minP2gScore,
            String signalType,
            String sortBy,
            String sortOrder,
            String p2gMode
    );

    List<RegulatoryAnnotationContextOption> getRegulatoryAnnotationContextOptions(
            String datasetId,
            String domain,
            String annotationType
    );

    void streamRegulatoryAnnotationsCsv(
            String datasetId,
            String domain,
            String annotationType,
            String targetGene,
            String peak,
            String regionType,
            String contextCellType,
            String contextCluster,
            Double maxFdr,
            Double minLog2fc,
            Double minP2gScore,
            String signalType,
            String sortBy,
            String sortOrder,
            String p2gMode,
            String sampleLabel,
            OutputStream outputStream
    ) throws IOException;

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
