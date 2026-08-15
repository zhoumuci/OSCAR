package com.oscar.backend.entity;

import java.util.List;

public class GeneSearchRequest {
    private List<String> genes;
    private String sortBy = "sampleName";
    private String matchMode = "union";
    private int resultSize = 0; // 0 = all
    private String domain; // null = no filter
    private String signalType; // null = no filter, "gene_expression" or "gene_score"
    private String tissue; // null = no filter

    public List<String> getGenes() { return genes; }
    public void setGenes(List<String> v) { this.genes = v; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String v) { this.sortBy = v; }
    public String getMatchMode() { return matchMode; } public void setMatchMode(String v) { this.matchMode = v; }
    public int getResultSize() { return resultSize; }
    public void setResultSize(int v) { this.resultSize = v; }
    public String getDomain() { return domain; }
    public void setDomain(String v) { this.domain = v; }
    public String getSignalType() { return signalType; }
    public void setSignalType(String v) { this.signalType = v; }
    public String getTissue() { return tissue; }
    public void setTissue(String v) { this.tissue = v; }
}
