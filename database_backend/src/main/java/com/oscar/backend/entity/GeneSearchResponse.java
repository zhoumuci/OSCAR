package com.oscar.backend.entity;

import java.util.List;

public class GeneSearchResponse {
    private Summary summary;
    private List<SampleItem> samples;

    public static class Summary {
        private int matchedSamples;
        private long markerGeneEvidence;
        private int linkedPeaks;
        public int getMatchedSamples() { return matchedSamples; } public void setMatchedSamples(int v) { this.matchedSamples = v; }
        public long getMarkerGeneEvidence() { return markerGeneEvidence; } public void setMarkerGeneEvidence(long v) { this.markerGeneEvidence = v; }
        public int getLinkedPeaks() { return linkedPeaks; } public void setLinkedPeaks(int v) { this.linkedPeaks = v; }
    }

    public static class SampleItem {
        private String sampleId;
        private String sampleName;
        private String tissue;
        private String cellContext;
        private int matchedGenes;
        private long matchedCells;
        private int linkedPeaks;
        private boolean hasAtac;
        private boolean hasRna;
        private int cellCount;
        private String platform;
        private String sourceId;
        private String disease;
        private String sampleSource;
        public String getSampleId() { return sampleId; } public void setSampleId(String v) { this.sampleId = v; }
        public String getSampleName() { return sampleName; } public void setSampleName(String v) { this.sampleName = v; }
        public String getTissue() { return tissue; } public void setTissue(String v) { this.tissue = v; }
        public String getCellContext() { return cellContext; } public void setCellContext(String v) { this.cellContext = v; }
        public int getMatchedGenes() { return matchedGenes; } public void setMatchedGenes(int v) { this.matchedGenes = v; }
        public long getMatchedCells() { return matchedCells; } public void setMatchedCells(long v) { this.matchedCells = v; }
        public int getLinkedPeaks() { return linkedPeaks; } public void setLinkedPeaks(int v) { this.linkedPeaks = v; }
        public boolean isHasAtac() { return hasAtac; } public void setHasAtac(boolean v) { this.hasAtac = v; }
        public boolean isHasRna() { return hasRna; } public void setHasRna(boolean v) { this.hasRna = v; }
        public int getCellCount() { return cellCount; } public void setCellCount(int v) { this.cellCount = v; }
        public String getPlatform() { return platform; } public void setPlatform(String v) { this.platform = v; }
        public String getSourceId() { return sourceId; } public void setSourceId(String v) { this.sourceId = v; }
        public String getDisease() { return disease; } public void setDisease(String v) { this.disease = v; }
        public String getSampleSource() { return sampleSource; } public void setSampleSource(String v) { this.sampleSource = v; }
    }

    public Summary getSummary() { return summary; } public void setSummary(Summary v) { this.summary = v; }
    public List<SampleItem> getSamples() { return samples; } public void setSamples(List<SampleItem> v) { this.samples = v; }
}
