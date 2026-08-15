package com.oscar.backend.entity;

import java.util.List;

public class PeakGeneContextRequest {

    private List<PeakInput> peaks;
    private List<String> genes;
    private String tissue;
    private String datasetId;
    private String referenceMode;
    private String resultType;
    private Advanced advanced;

    public static class PeakInput {
        private String chrom;
        private long start;
        private long end;
        public String getChrom() { return chrom; }
        public void setChrom(String v) { this.chrom = v; }
        public long getStart() { return start; }
        public void setStart(long v) { this.start = v; }
        public long getEnd() { return end; }
        public void setEnd(long v) { this.end = v; }
    }

    public static class Advanced {
        private int minOverlapBp;
        private Integer maxReturnedLinks;
        public int getMinOverlapBp() { return minOverlapBp; }
        public void setMinOverlapBp(int v) { this.minOverlapBp = v; }
        public Integer getMaxReturnedLinks() { return maxReturnedLinks; }
        public void setMaxReturnedLinks(Integer v) { this.maxReturnedLinks = v; }
    }

    public List<PeakInput> getPeaks() { return peaks; }
    public void setPeaks(List<PeakInput> v) { this.peaks = v; }
    public List<String> getGenes() { return genes; }
    public void setGenes(List<String> v) { this.genes = v; }
    public String getTissue() { return tissue; }
    public void setTissue(String v) { this.tissue = v; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String v) { this.datasetId = v; }
    public String getReferenceMode() { return referenceMode; }
    public void setReferenceMode(String v) { this.referenceMode = v; }
    public String getResultType() { return resultType; }
    public void setResultType(String v) { this.resultType = v; }
    public Advanced getAdvanced() { return advanced; }
    public void setAdvanced(Advanced v) { this.advanced = v; }
}
