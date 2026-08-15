package com.oscar.backend.entity;

public class SequencePeak2GeneRequest {

    private String sequence;
    private String genomeBuild = "hg38";
    private String referenceScope = "all";
    private String datasetId;
    private String resultContent = "all";
    private String blastTask = "auto";
    private Integer maxTargetSeqs = 500;
    private Integer maxHsps = 200;
    private Double evalueCutoff = 10.0;
    private Integer flankBp = 0;
    private Integer limit = 0;

    public String getSequence() { return sequence; }
    public void setSequence(String v) { this.sequence = v; }

    public String getGenomeBuild() { return genomeBuild; }
    public void setGenomeBuild(String v) { this.genomeBuild = v; }

    public String getReferenceScope() { return referenceScope; }
    public void setReferenceScope(String v) { this.referenceScope = v; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String v) { this.datasetId = v; }

    public String getResultContent() { return resultContent; }
    public void setResultContent(String v) { this.resultContent = v; }

    public String getBlastTask() { return blastTask; }
    public void setBlastTask(String v) { this.blastTask = v; }

    public Integer getMaxTargetSeqs() { return maxTargetSeqs; }
    public void setMaxTargetSeqs(Integer v) { this.maxTargetSeqs = v; }

    public Integer getMaxHsps() { return maxHsps; }
    public void setMaxHsps(Integer v) { this.maxHsps = v; }

    public Double getEvalueCutoff() { return evalueCutoff; }
    public void setEvalueCutoff(Double v) { this.evalueCutoff = v; }

    public Integer getFlankBp() { return flankBp; }
    public void setFlankBp(Integer v) { this.flankBp = v; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer v) { this.limit = v; }
}
