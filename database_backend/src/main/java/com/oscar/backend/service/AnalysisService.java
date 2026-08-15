package com.oscar.backend.service;

import com.oscar.backend.entity.SequencePeak2GeneRequest;
import com.oscar.backend.entity.SequencePeak2GeneResponse;
import com.oscar.backend.entity.SequencePeak2GeneEvidenceRequest;
import com.oscar.backend.entity.SequencePeak2GeneEvidenceResponse;
import com.oscar.backend.entity.CellTypeEnrichmentRequest;
import com.oscar.backend.entity.CellTypeEnrichmentResponse;

import java.util.List;
import java.util.Map;

public interface AnalysisService {

    List<Map<String, String>> getAllDatasetIds();

    SequencePeak2GeneResponse sequencePeak2Gene(SequencePeak2GeneRequest request);

    SequencePeak2GeneResponse sequencePeak2Gene(
            SequencePeak2GeneRequest request,
            SequenceAnalysisProgressListener progressListener
    );

    SequencePeak2GeneEvidenceResponse sequencePeak2GeneEvidence(SequencePeak2GeneEvidenceRequest request);

    CellTypeEnrichmentResponse cellTypeEnrichment(CellTypeEnrichmentRequest request);

    List<String> getCellTypeEnrichmentTissues();

    List<Map<String, Object>> getCellTypeEnrichmentDatasets(String tissue);

    List<Map<String, Object>> getSamplesByCellType(String cellType);

    List<Map<String, Object>> getSamplesByTissue(String tissue);
}
