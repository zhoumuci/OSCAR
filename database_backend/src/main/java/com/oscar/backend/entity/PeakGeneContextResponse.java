package com.oscar.backend.entity;

import java.util.List;

public class PeakGeneContextResponse {

    private Summary summary;
    private List<CellTypeResult> cellTypeResults;
    private List<PairDetail> pairs;
    private NetworkData networkData;

    public static class Summary {
        private int totalPairs;
        private int uniquePeaks;
        private int uniqueGenes;
        private int uniqueDatasets;
        private int uniqueCellTypes;
        private String topCellType;
        private int topCellTypeEvidence;
        public int getTotalPairs() { return totalPairs; } public void setTotalPairs(int v) { this.totalPairs = v; }
        public int getUniquePeaks() { return uniquePeaks; } public void setUniquePeaks(int v) { this.uniquePeaks = v; }
        public int getUniqueGenes() { return uniqueGenes; } public void setUniqueGenes(int v) { this.uniqueGenes = v; }
        public int getUniqueDatasets() { return uniqueDatasets; } public void setUniqueDatasets(int v) { this.uniqueDatasets = v; }
        public int getUniqueCellTypes() { return uniqueCellTypes; } public void setUniqueCellTypes(int v) { this.uniqueCellTypes = v; }
        public String getTopCellType() { return topCellType; } public void setTopCellType(String v) { this.topCellType = v; }
        public int getTopCellTypeEvidence() { return topCellTypeEvidence; } public void setTopCellTypeEvidence(int v) { this.topCellTypeEvidence = v; }
    }

    public static class CellTypeResult {
        private String cellType;
        private int evidenceCount;
        private int peakCount;
        private int geneCount;
        private int datasetCount;
        private List<GeneDetail> geneDetails;
        public String getCellType() { return cellType; } public void setCellType(String v) { this.cellType = v; }
        public int getEvidenceCount() { return evidenceCount; } public void setEvidenceCount(int v) { this.evidenceCount = v; }
        public int getPeakCount() { return peakCount; } public void setPeakCount(int v) { this.peakCount = v; }
        public int getGeneCount() { return geneCount; } public void setGeneCount(int v) { this.geneCount = v; }
        public int getDatasetCount() { return datasetCount; } public void setDatasetCount(int v) { this.datasetCount = v; }
        public List<GeneDetail> getGeneDetails() { return geneDetails; } public void setGeneDetails(List<GeneDetail> v) { this.geneDetails = v; }
    }

    public static class GeneDetail {
        private String gene;
        private int count;
        public String getGene() { return gene; } public void setGene(String v) { this.gene = v; }
        public int getCount() { return count; } public void setCount(int v) { this.count = v; }
    }

    public static class PairDetail {
        private String peakName;
        private String chromosome;
        private long peakStart;
        private long peakEnd;
        private String geneName;
        private String cellType;
        private String contextLabel;
        private String clusterLabel;
        private String datasetId;
        private Double linkScore;
        private Double linkFdr;
        private boolean hasMarkerPeak;
        private boolean hasMarkerGene;
        private String signalType;
        private List<String> geneMarkerTypes;
        public String getPeakName() { return peakName; } public void setPeakName(String v) { this.peakName = v; }
        public String getChromosome() { return chromosome; } public void setChromosome(String v) { this.chromosome = v; }
        public long getPeakStart() { return peakStart; } public void setPeakStart(long v) { this.peakStart = v; }
        public long getPeakEnd() { return peakEnd; } public void setPeakEnd(long v) { this.peakEnd = v; }
        public String getGeneName() { return geneName; } public void setGeneName(String v) { this.geneName = v; }
        public String getCellType() { return cellType; } public void setCellType(String v) { this.cellType = v; }
        public String getContextLabel() { return contextLabel; } public void setContextLabel(String v) { this.contextLabel = v; }
        public String getClusterLabel() { return clusterLabel; } public void setClusterLabel(String v) { this.clusterLabel = v; }
        public String getDatasetId() { return datasetId; } public void setDatasetId(String v) { this.datasetId = v; }
        public Double getLinkScore() { return linkScore; } public void setLinkScore(Double v) { this.linkScore = v; }
        public Double getLinkFdr() { return linkFdr; } public void setLinkFdr(Double v) { this.linkFdr = v; }
        public boolean isHasMarkerPeak() { return hasMarkerPeak; } public void setHasMarkerPeak(boolean v) { this.hasMarkerPeak = v; }
        public boolean isHasMarkerGene() { return hasMarkerGene; } public void setHasMarkerGene(boolean v) { this.hasMarkerGene = v; }
        public String getSignalType() { return signalType; } public void setSignalType(String v) { this.signalType = v; }
        public List<String> getGeneMarkerTypes() { return geneMarkerTypes; } public void setGeneMarkerTypes(List<String> v) { this.geneMarkerTypes = v; }
    }

    public static class NetworkData {
        private List<NetworkNode> nodes;
        private List<NetworkEdge> edges;
        private int peakLimitPerGene;
        public List<NetworkNode> getNodes() { return nodes; } public void setNodes(List<NetworkNode> v) { this.nodes = v; }
        public List<NetworkEdge> getEdges() { return edges; } public void setEdges(List<NetworkEdge> v) { this.edges = v; }
        public int getPeakLimitPerGene() { return peakLimitPerGene; } public void setPeakLimitPerGene(int v) { this.peakLimitPerGene = v; }
    }

    public static class NetworkNode {
        private String id;
        private String name;
        private String category;  // peak / gene / cellType
        private int value;        // size metric
        public String getId() { return id; } public void setId(String v) { this.id = v; }
        public String getName() { return name; } public void setName(String v) { this.name = v; }
        public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
        public int getValue() { return value; } public void setValue(int v) { this.value = v; }
    }

    public static class NetworkEdge {
        private String source;
        private String target;
        private Double weight;
        private int evidenceCount;
        public String getSource() { return source; } public void setSource(String v) { this.source = v; }
        public String getTarget() { return target; } public void setTarget(String v) { this.target = v; }
        public Double getWeight() { return weight; } public void setWeight(Double v) { this.weight = v; }
        public int getEvidenceCount() { return evidenceCount; } public void setEvidenceCount(int v) { this.evidenceCount = v; }
    }

    public Summary getSummary() { return summary; } public void setSummary(Summary v) { this.summary = v; }
    public List<CellTypeResult> getCellTypeResults() { return cellTypeResults; } public void setCellTypeResults(List<CellTypeResult> v) { this.cellTypeResults = v; }
    public List<PairDetail> getPairs() { return pairs; } public void setPairs(List<PairDetail> v) { this.pairs = v; }
    public NetworkData getNetworkData() { return networkData; } public void setNetworkData(NetworkData v) { this.networkData = v; }
}
