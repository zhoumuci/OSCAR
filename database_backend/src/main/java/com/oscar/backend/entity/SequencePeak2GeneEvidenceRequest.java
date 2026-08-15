package com.oscar.backend.entity;

public class SequencePeak2GeneEvidenceRequest {

    private String hitId;
    private Integer hitRank;
    private String chromosome;
    private Long start;
    private Long end;
    private String strand;
    private String referenceScope = "all";
    private String datasetId;
    private String resultContent = "all";
    private Integer flankBp = 0;
    private Integer limit = 0;

    public String getHitId() { return hitId; }
    public void setHitId(String v) { this.hitId = v; }
    public Integer getHitRank() { return hitRank; }
    public void setHitRank(Integer v) { this.hitRank = v; }
    public String getChromosome() { return chromosome; }
    public void setChromosome(String v) { this.chromosome = v; }
    public Long getStart() { return start; }
    public void setStart(Long v) { this.start = v; }
    public Long getEnd() { return end; }
    public void setEnd(Long v) { this.end = v; }
    public String getStrand() { return strand; }
    public void setStrand(String v) { this.strand = v; }
    public String getReferenceScope() { return referenceScope; }
    public void setReferenceScope(String v) { this.referenceScope = v; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String v) { this.datasetId = v; }
    public String getResultContent() { return resultContent; }
    public void setResultContent(String v) { this.resultContent = v; }
    public Integer getFlankBp() { return flankBp; }
    public void setFlankBp(Integer v) { this.flankBp = v; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer v) { this.limit = v; }
}
