package com.oscar.backend.entity;

import java.util.List;

public class PeakSearchRequest {
    private List<Region> regions;
    private String matchMode = "any";
    private String domain = "integration";
    private String datasetId;

    public List<Region> getRegions() { return regions; }
    public void setRegions(List<Region> regions) { this.regions = regions; }
    public String getMatchMode() { return matchMode; }
    public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public static class Region {
        private String chrom;
        private long start;
        private long end;

        public String getChrom() { return chrom; }
        public void setChrom(String chrom) { this.chrom = chrom; }
        public long getStart() { return start; }
        public void setStart(long start) { this.start = start; }
        public long getEnd() { return end; }
        public void setEnd(long end) { this.end = end; }
    }
}
