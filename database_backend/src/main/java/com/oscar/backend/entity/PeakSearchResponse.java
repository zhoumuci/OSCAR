package com.oscar.backend.entity;

import java.util.List;

public class PeakSearchResponse {
    private Summary summary;
    private List<SampleItem> samples;

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<SampleItem> getSamples() { return samples; }
    public void setSamples(List<SampleItem> samples) { this.samples = samples; }

    public static class Summary {
        private int inputRegions;
        private int matchedSamples;
        private int matchedInputRegions;
        private long overlappingPeaks;
        private long linkedGenes;

        public int getInputRegions() { return inputRegions; }
        public void setInputRegions(int inputRegions) { this.inputRegions = inputRegions; }
        public int getMatchedSamples() { return matchedSamples; }
        public void setMatchedSamples(int matchedSamples) { this.matchedSamples = matchedSamples; }
        public int getMatchedInputRegions() { return matchedInputRegions; }
        public void setMatchedInputRegions(int matchedInputRegions) { this.matchedInputRegions = matchedInputRegions; }
        public long getOverlappingPeaks() { return overlappingPeaks; }
        public void setOverlappingPeaks(long overlappingPeaks) { this.overlappingPeaks = overlappingPeaks; }
        public long getLinkedGenes() { return linkedGenes; }
        public void setLinkedGenes(long linkedGenes) { this.linkedGenes = linkedGenes; }
    }

    public static class SampleItem {
        private String sampleId;
        private String sampleName;
        private String tissue;
        private String cellContext;
        private int cellCount;
        private String platform;
        private String sourceId;
        private String disease;
        private String sampleSource;
        private int matchedRegions;
        private long overlappingPeaks;
        private long linkedGenes;
        private boolean hasAtac;
        private boolean hasP2g;

        public String getSampleId() { return sampleId; }
        public void setSampleId(String sampleId) { this.sampleId = sampleId; }
        public String getSampleName() { return sampleName; }
        public void setSampleName(String sampleName) { this.sampleName = sampleName; }
        public String getTissue() { return tissue; }
        public void setTissue(String tissue) { this.tissue = tissue; }
        public String getCellContext() { return cellContext; }
        public void setCellContext(String cellContext) { this.cellContext = cellContext; }
        public int getCellCount() { return cellCount; }
        public void setCellCount(int cellCount) { this.cellCount = cellCount; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getDisease() { return disease; }
        public void setDisease(String disease) { this.disease = disease; }
        public String getSampleSource() { return sampleSource; }
        public void setSampleSource(String sampleSource) { this.sampleSource = sampleSource; }
        public int getMatchedRegions() { return matchedRegions; }
        public void setMatchedRegions(int matchedRegions) { this.matchedRegions = matchedRegions; }
        public long getOverlappingPeaks() { return overlappingPeaks; }
        public void setOverlappingPeaks(long overlappingPeaks) { this.overlappingPeaks = overlappingPeaks; }
        public long getLinkedGenes() { return linkedGenes; }
        public void setLinkedGenes(long linkedGenes) { this.linkedGenes = linkedGenes; }
        public boolean isHasAtac() { return hasAtac; }
        public void setHasAtac(boolean hasAtac) { this.hasAtac = hasAtac; }
        public boolean isHasP2g() { return hasP2g; }
        public void setHasP2g(boolean hasP2g) { this.hasP2g = hasP2g; }
    }
}
