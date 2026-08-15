package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class FeatureOccurrenceResponse {

    private String featureType;
    private String featureId;
    private String domain;
    // gene-level genomic context
    private String genomeBuild;
    private String geneBodyRegion;
    private String promoterRegion;
    private List<GeneEnhancerSummary> enhancerSummaries = new ArrayList<>();
    private int datasetCount;
    private int cellTypeCount;
    private int clusterCount;
    private int totalOccurrences;
    private List<FeatureOccurrenceTopCellType> topCellTypes = new ArrayList<>();
    private List<FeatureOccurrenceDatasetEntry> datasets = new ArrayList<>();
    private List<DatasetRankingItem> datasetRanking = new ArrayList<>();
    private List<CellContextRankingItem> cellContextRanking = new ArrayList<>();
    private boolean available;
    private String message;

    // getters/setters
    public String getFeatureType() { return featureType; }
    public void setFeatureType(String featureType) { this.featureType = featureType; }
    public String getFeatureId() { return featureId; }
    public void setFeatureId(String featureId) { this.featureId = featureId; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getGenomeBuild() { return genomeBuild; }
    public void setGenomeBuild(String v) { this.genomeBuild = v; }
    public String getGeneBodyRegion() { return geneBodyRegion; }
    public void setGeneBodyRegion(String v) { this.geneBodyRegion = v; }
    public String getPromoterRegion() { return promoterRegion; }
    public void setPromoterRegion(String v) { this.promoterRegion = v; }
    public List<GeneEnhancerSummary> getEnhancerSummaries() { return enhancerSummaries; }
    public void setEnhancerSummaries(List<GeneEnhancerSummary> v) { this.enhancerSummaries = v; }
    public int getDatasetCount() { return datasetCount; }
    public void setDatasetCount(int datasetCount) { this.datasetCount = datasetCount; }
    public int getCellTypeCount() { return cellTypeCount; }
    public void setCellTypeCount(int cellTypeCount) { this.cellTypeCount = cellTypeCount; }
    public int getClusterCount() { return clusterCount; }
    public void setClusterCount(int clusterCount) { this.clusterCount = clusterCount; }
    public int getTotalOccurrences() { return totalOccurrences; }
    public void setTotalOccurrences(int totalOccurrences) { this.totalOccurrences = totalOccurrences; }
    public List<FeatureOccurrenceTopCellType> getTopCellTypes() { return topCellTypes; }
    public void setTopCellTypes(List<FeatureOccurrenceTopCellType> topCellTypes) { this.topCellTypes = topCellTypes; }
    public List<FeatureOccurrenceDatasetEntry> getDatasets() { return datasets; }
    public void setDatasets(List<FeatureOccurrenceDatasetEntry> datasets) { this.datasets = datasets; }
    public List<DatasetRankingItem> getDatasetRanking() { return datasetRanking; }
    public void setDatasetRanking(List<DatasetRankingItem> datasetRanking) { this.datasetRanking = datasetRanking; }
    public List<CellContextRankingItem> getCellContextRanking() { return cellContextRanking; }
    public void setCellContextRanking(List<CellContextRankingItem> cellContextRanking) { this.cellContextRanking = cellContextRanking; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static FeatureOccurrenceResponse unavailable(String featureType, String featureId, String domain, String message) {
        FeatureOccurrenceResponse r = new FeatureOccurrenceResponse();
        r.featureType = featureType;
        r.featureId = featureId;
        r.domain = domain;
        r.available = false;
        r.message = message;
        return r;
    }

    // nested types
    public static class FeatureOccurrenceTopCellType {
        private String cellType;
        private int count;
        public String getCellType() { return cellType; }
        public void setCellType(String cellType) { this.cellType = cellType; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class FeatureOccurrenceDatasetEntry {
        private String datasetId;
        private String cellType;
        private String cluster;
        private int occurrenceCount;
        public String getDatasetId() { return datasetId; }
        public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
        public String getCellType() { return cellType; }
        public void setCellType(String cellType) { this.cellType = cellType; }
        public String getCluster() { return cluster; }
        public void setCluster(String cluster) { this.cluster = cluster; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    }

    public static class DatasetRankingItem {
        private String datasetId;
        private String sampleName;
        private int recordCount;
        private int cellContextCount;
        private int clusterCount;
        public String getDatasetId() { return datasetId; }
        public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
        public String getSampleName() { return sampleName; }
        public void setSampleName(String sampleName) { this.sampleName = sampleName; }
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        public int getCellContextCount() { return cellContextCount; }
        public void setCellContextCount(int cellContextCount) { this.cellContextCount = cellContextCount; }
        public int getClusterCount() { return clusterCount; }
        public void setClusterCount(int clusterCount) { this.clusterCount = clusterCount; }
    }

    public static class CellContextRankingItem {
        private String cellType;
        private int recordCount;
        private int datasetCount;
        private int clusterCount;
        public String getCellType() { return cellType; }
        public void setCellType(String cellType) { this.cellType = cellType; }
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        public int getDatasetCount() { return datasetCount; }
        public void setDatasetCount(int datasetCount) { this.datasetCount = datasetCount; }
        public int getClusterCount() { return clusterCount; }
        public void setClusterCount(int clusterCount) { this.clusterCount = clusterCount; }
    }

    public static class GeneEnhancerSummary {
        private String enhancerType;   // SE or TE
        private String label;          // Super enhancer / Typical enhancer
        private int matchedRegionCount;
        private int biosampleCount;
        private List<String> exampleBiosamples = new ArrayList<>();
        private List<String> exampleRegions = new ArrayList<>();
        public String getEnhancerType() { return enhancerType; }
        public void setEnhancerType(String v) { this.enhancerType = v; }
        public String getLabel() { return label; }
        public void setLabel(String v) { this.label = v; }
        public int getMatchedRegionCount() { return matchedRegionCount; }
        public void setMatchedRegionCount(int v) { this.matchedRegionCount = v; }
        public int getBiosampleCount() { return biosampleCount; }
        public void setBiosampleCount(int v) { this.biosampleCount = v; }
        public List<String> getExampleBiosamples() { return exampleBiosamples; }
        public void setExampleBiosamples(List<String> v) { this.exampleBiosamples = v; }
        public List<String> getExampleRegions() { return exampleRegions; }
        public void setExampleRegions(List<String> v) { this.exampleRegions = v; }
    }
}
