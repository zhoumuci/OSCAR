package com.oscar.backend.entity;

public class RegulatoryAnnotationRow {

    private String recordKind;
    private Long linkedRegionId;
    private Long markerGeneId;
    private Long markerPeakId;
    private Long peakGeneLinkId;
    private String datasetId;
    private String domain;
    private String groupName;
    private String geneSymbol;
    private String geneId;
    private String geneChromosome;
    private Long geneStart;
    private Long geneEnd;
    private String strand;
    private Double avgLog2fc;
    private Double geneFdr;
    private Double geneMeanDiff;
    private String markerGeneSourceFile;
    private String signalType;
    private String peakName;
    private String peakRegion;
    private String peakChromosome;
    private Long peakStart;
    private Long peakEnd;
    private Double peakLog2fc;
    private Double peakFdr;
    private Double peakMeanDiff;
    private String markerPeakSourceFile;
    private String linkedGeneName;
    private String cellType;
    private String clusterLabel;
    private String contextLabel;
    private String tfName;
    private Double linkScore;
    private Long linkedGeneCount;
    private Double correlation;
    private Double linkFdr;
    private Double varQrna;
    private Double varQatac;
    private String linkSource;

    public String getRecordKind() {
        return recordKind;
    }

    public void setRecordKind(String recordKind) {
        this.recordKind = recordKind;
    }

    public Long getLinkedRegionId() {
        return linkedRegionId;
    }

    public void setLinkedRegionId(Long linkedRegionId) {
        this.linkedRegionId = linkedRegionId;
    }

    public Long getMarkerGeneId() {
        return markerGeneId;
    }

    public void setMarkerGeneId(Long markerGeneId) {
        this.markerGeneId = markerGeneId;
    }

    public Long getMarkerPeakId() {
        return markerPeakId;
    }

    public void setMarkerPeakId(Long markerPeakId) {
        this.markerPeakId = markerPeakId;
    }

    public Long getPeakGeneLinkId() {
        return peakGeneLinkId;
    }

    public void setPeakGeneLinkId(Long peakGeneLinkId) {
        this.peakGeneLinkId = peakGeneLinkId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getGeneChromosome() {
        return geneChromosome;
    }

    public void setGeneChromosome(String geneChromosome) {
        this.geneChromosome = geneChromosome;
    }

    public Long getGeneStart() {
        return geneStart;
    }

    public void setGeneStart(Long geneStart) {
        this.geneStart = geneStart;
    }

    public Long getGeneEnd() {
        return geneEnd;
    }

    public void setGeneEnd(Long geneEnd) {
        this.geneEnd = geneEnd;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public Double getAvgLog2fc() {
        return avgLog2fc;
    }

    public void setAvgLog2fc(Double avgLog2fc) {
        this.avgLog2fc = avgLog2fc;
    }

    public Double getGeneFdr() {
        return geneFdr;
    }

    public void setGeneFdr(Double geneFdr) {
        this.geneFdr = geneFdr;
    }

    public Double getGeneMeanDiff() {
        return geneMeanDiff;
    }

    public void setGeneMeanDiff(Double geneMeanDiff) {
        this.geneMeanDiff = geneMeanDiff;
    }

    public String getMarkerGeneSourceFile() {
        return markerGeneSourceFile;
    }

    public void setMarkerGeneSourceFile(String markerGeneSourceFile) {
        this.markerGeneSourceFile = markerGeneSourceFile;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public String getPeakName() {
        return peakName;
    }

    public void setPeakName(String peakName) {
        this.peakName = peakName;
    }

    public String getPeakRegion() {
        return peakRegion;
    }

    public void setPeakRegion(String peakRegion) {
        this.peakRegion = peakRegion;
    }

    public String getPeakChromosome() {
        return peakChromosome;
    }

    public void setPeakChromosome(String peakChromosome) {
        this.peakChromosome = peakChromosome;
    }

    public Long getPeakStart() {
        return peakStart;
    }

    public void setPeakStart(Long peakStart) {
        this.peakStart = peakStart;
    }

    public Long getPeakEnd() {
        return peakEnd;
    }

    public void setPeakEnd(Long peakEnd) {
        this.peakEnd = peakEnd;
    }

    public Double getPeakLog2fc() {
        return peakLog2fc;
    }

    public void setPeakLog2fc(Double peakLog2fc) {
        this.peakLog2fc = peakLog2fc;
    }

    public Double getPeakFdr() {
        return peakFdr;
    }

    public void setPeakFdr(Double peakFdr) {
        this.peakFdr = peakFdr;
    }

    public Double getPeakMeanDiff() {
        return peakMeanDiff;
    }

    public void setPeakMeanDiff(Double peakMeanDiff) {
        this.peakMeanDiff = peakMeanDiff;
    }

    public String getMarkerPeakSourceFile() {
        return markerPeakSourceFile;
    }

    public void setMarkerPeakSourceFile(String markerPeakSourceFile) {
        this.markerPeakSourceFile = markerPeakSourceFile;
    }

    public String getLinkedGeneName() {
        return linkedGeneName;
    }

    public void setLinkedGeneName(String linkedGeneName) {
        this.linkedGeneName = linkedGeneName;
    }

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getClusterLabel() {
        return clusterLabel;
    }

    public void setClusterLabel(String clusterLabel) {
        this.clusterLabel = clusterLabel;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public void setContextLabel(String contextLabel) {
        this.contextLabel = contextLabel;
    }

    public String getTfName() {
        return tfName;
    }

    public void setTfName(String tfName) {
        this.tfName = tfName;
    }

    public Double getLinkScore() {
        return linkScore;
    }

    public void setLinkScore(Double linkScore) {
        this.linkScore = linkScore;
    }

    public Long getLinkedGeneCount() {
        return linkedGeneCount;
    }

    public void setLinkedGeneCount(Long linkedGeneCount) {
        this.linkedGeneCount = linkedGeneCount;
    }

    public Double getCorrelation() {
        return correlation;
    }

    public void setCorrelation(Double correlation) {
        this.correlation = correlation;
    }

    public Double getLinkFdr() {
        return linkFdr;
    }

    public void setLinkFdr(Double linkFdr) {
        this.linkFdr = linkFdr;
    }

    public Double getVarQrna() { return varQrna; }
    public void setVarQrna(Double varQrna) { this.varQrna = varQrna; }
    public Double getVarQatac() { return varQatac; }
    public void setVarQatac(Double varQatac) { this.varQatac = varQatac; }

    public String getLinkSource() {
        return linkSource;
    }

    public void setLinkSource(String linkSource) {
        this.linkSource = linkSource;
    }
}
