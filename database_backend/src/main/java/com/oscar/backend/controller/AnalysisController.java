package com.oscar.backend.controller;

import com.oscar.backend.entity.CellTypeEnrichmentRequest;
import com.oscar.backend.entity.CellTypeEnrichmentResponse;
import com.oscar.backend.entity.PeakGeneContextRequest;
import com.oscar.backend.entity.PeakGeneContextResponse;
import com.oscar.backend.entity.PeakGeneContextJobResponse;
import com.oscar.backend.entity.SequencePeak2GeneRequest;
import com.oscar.backend.entity.SequencePeak2GeneResponse;
import com.oscar.backend.entity.SequencePeak2GeneEvidenceRequest;
import com.oscar.backend.entity.SequencePeak2GeneEvidenceResponse;
import com.oscar.backend.entity.SequencePeak2GeneJobResponse;
import com.oscar.backend.service.AnalysisService;
import com.oscar.backend.service.PeakGeneContextService;
import com.oscar.backend.service.PeakGeneContextJobService;
import com.oscar.backend.service.SequencePeak2GeneJobService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final PeakGeneContextService peakGeneContextService;
    private final PeakGeneContextJobService peakGeneContextJobService;
    private final SequencePeak2GeneJobService sequenceJobService;

    public AnalysisController(
            AnalysisService analysisService,
            PeakGeneContextService peakGeneContextService,
            PeakGeneContextJobService peakGeneContextJobService,
            SequencePeak2GeneJobService sequenceJobService
    ) {
        this.analysisService = analysisService;
        this.peakGeneContextService = peakGeneContextService;
        this.peakGeneContextJobService = peakGeneContextJobService;
        this.sequenceJobService = sequenceJobService;
    }

    @GetMapping("/datasets")
    public List<Map<String, String>> getAllDatasets() {
        return analysisService.getAllDatasetIds();
    }

    @PostMapping("/sequence-peak2gene")
    public SequencePeak2GeneResponse sequencePeak2Gene(@RequestBody SequencePeak2GeneRequest request) {
        return analysisService.sequencePeak2Gene(request);
    }

    @PostMapping("/sequence-peak2gene/jobs")
    public ResponseEntity<SequencePeak2GeneJobResponse> submitSequencePeak2GeneJob(
            @RequestBody SequencePeak2GeneRequest request
    ) {
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(sequenceJobService.submit(request));
    }

    @GetMapping("/sequence-peak2gene/jobs/{jobId}")
    public ResponseEntity<SequencePeak2GeneJobResponse> getSequencePeak2GeneJob(@PathVariable String jobId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sequenceJobService.get(jobId));
    }

    @PostMapping("/sequence-peak2gene/evidence")
    public SequencePeak2GeneEvidenceResponse sequencePeak2GeneEvidence(
            @RequestBody SequencePeak2GeneEvidenceRequest request
    ) {
        return analysisService.sequencePeak2GeneEvidence(request);
    }

    @PostMapping("/peak-gene-context")
    public PeakGeneContextResponse peakGeneContext(@RequestBody PeakGeneContextRequest request) {
        return peakGeneContextService.analyze(request);
    }

    @GetMapping("/peak-gene-context/tissues")
    public List<String> peakGeneContextTissues() {
        return peakGeneContextService.getTissues();
    }

    @GetMapping("/peak-gene-context/datasets")
    public List<Map<String, Object>> peakGeneContextDatasets(@RequestParam String tissue) {
        return peakGeneContextService.getDatasets(tissue);
    }

    @PostMapping("/peak-gene-context/jobs")
    public ResponseEntity<PeakGeneContextJobResponse> submitPeakGeneContextJob(
            @RequestBody PeakGeneContextRequest request
    ) {
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(peakGeneContextJobService.submit(request));
    }

    @GetMapping("/peak-gene-context/jobs/{jobId}")
    public ResponseEntity<PeakGeneContextJobResponse> getPeakGeneContextJob(@PathVariable String jobId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(peakGeneContextJobService.get(jobId));
    }

    @PostMapping("/cell-type-enrichment")
    public CellTypeEnrichmentResponse cellTypeEnrichment(@RequestBody CellTypeEnrichmentRequest request) {
        return analysisService.cellTypeEnrichment(request);
    }

    @GetMapping("/cell-type-enrichment/tissues")
    public List<String> cellTypeEnrichmentTissues() {
        return analysisService.getCellTypeEnrichmentTissues();
    }

    @GetMapping("/cell-type-enrichment/datasets")
    public List<Map<String, Object>> cellTypeEnrichmentDatasets(@RequestParam String tissue) {
        return analysisService.getCellTypeEnrichmentDatasets(tissue);
    }

    @GetMapping("/cell-type-samples")
    public List<Map<String, Object>> getCellTypeSamples(@RequestParam String q) {
        return analysisService.getSamplesByCellType(q);
    }

    @GetMapping("/tissue-samples")
    public List<Map<String, Object>> getTissueSamples(@RequestParam String q) {
        return analysisService.getSamplesByTissue(q);
    }
}
