package com.oscar.backend.entity;

public class RegulatoryAnnotationRecord {

    private String id;
    private String annotationType;
    private String signalType;
    private Double varQrna;
    private Double varQatac;
    private String datasetId;
    private String domain;
    private String context;
    private String cellType;
    private String clusterLabel;
    private String source;
    private String evidence;
    private String targetGene;
    private String geneSymbol;
    private String geneId;
    private String geneChromosome;
    private Long geneStart;
    private Long geneEnd;
    private String geneRegion;
    private String strand;
    private String promoterRegion;
    private Double geneLog2fc;
    private Double geneFdr;
    private Double geneMeanDiff;
    private String peakName;
    private String peakChromosome;
    private Long peakStart;
    private Long peakEnd;
    private String peakRegion;
    private Double peakLog2fc;
    private Double peakFdr;
    private Double peakMeanDiff;
    private String linkedPeak;
    private String linkedGene;
    private Long linkedGeneCount;
    private Double linkScore;
    private Double correlation;
    private Double linkFdr;
    private Long distance;
    private String regionType;
    private String regulatoryRegion;
    private String tf;
    private String motifName;
    private String motifId;
    private String motifSource;
    private String motifLogoUrl;
    private String tfbsRegion;
    private Double genie3Weight;
    private Double nes;
    private String promoterTf;
    private String seTf;
    private String teTf;
    private String chromosome;
    private Long start;
    private Long end;
    private String region;
    private String peak;
    private String peakId;
    private String gene;
    private Double logFc;
    private Double log2fc;
    private Double fdr;
    private Double meanDiff;
    private Double adjustedPValue;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public Double getVarQrna() { return varQrna; }
    public void setVarQrna(Double varQrna) { this.varQrna = varQrna; }
    public Double getVarQatac() { return varQatac; }
    public void setVarQatac(Double varQatac) { this.varQatac = varQatac; }

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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getTargetGene() {
        return targetGene;
    }

    public void setTargetGene(String targetGene) {
        this.targetGene = targetGene;
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

    public String getGeneRegion() {
        return geneRegion;
    }

    public void setGeneRegion(String geneRegion) {
        this.geneRegion = geneRegion;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getPromoterRegion() {
        return promoterRegion;
    }

    public void setPromoterRegion(String promoterRegion) {
        this.promoterRegion = promoterRegion;
    }

    public Double getGeneLog2fc() {
        return geneLog2fc;
    }

    public void setGeneLog2fc(Double geneLog2fc) {
        this.geneLog2fc = geneLog2fc;
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

    public String getPeakName() {
        return peakName;
    }

    public void setPeakName(String peakName) {
        this.peakName = peakName;
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

    public String getPeakRegion() {
        return peakRegion;
    }

    public void setPeakRegion(String peakRegion) {
        this.peakRegion = peakRegion;
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

    public String getLinkedPeak() {
        return linkedPeak;
    }

    public void setLinkedPeak(String linkedPeak) {
        this.linkedPeak = linkedPeak;
    }

    public String getLinkedGene() {
        return linkedGene;
    }

    public void setLinkedGene(String linkedGene) {
        this.linkedGene = linkedGene;
    }

    public Long getLinkedGeneCount() {
        return linkedGeneCount;
    }

    public void setLinkedGeneCount(Long linkedGeneCount) {
        this.linkedGeneCount = linkedGeneCount;
    }

    public Double getLinkScore() {
        return linkScore;
    }

    public void setLinkScore(Double linkScore) {
        this.linkScore = linkScore;
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

    public Long getDistance() {
        return distance;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public String getRegionType() {
        return regionType;
    }

    public void setRegionType(String regionType) {
        this.regionType = regionType;
    }

    public String getRegulatoryRegion() {
        return regulatoryRegion;
    }

    public void setRegulatoryRegion(String regulatoryRegion) {
        this.regulatoryRegion = regulatoryRegion;
    }

    public String getTf() {
        return tf;
    }

    public void setTf(String tf) {
        this.tf = tf;
    }

    public String getMotifName() {
        return motifName;
    }

    public void setMotifName(String motifName) {
        this.motifName = motifName;
    }

    public String getMotifId() {
        return motifId;
    }

    public void setMotifId(String motifId) {
        this.motifId = motifId;
    }

    public String getMotifSource() {
        return motifSource;
    }

    public void setMotifSource(String motifSource) {
        this.motifSource = motifSource;
    }

    public String getMotifLogoUrl() {
        return motifLogoUrl;
    }

    public void setMotifLogoUrl(String motifLogoUrl) {
        this.motifLogoUrl = motifLogoUrl;
    }

    public String getTfbsRegion() {
        return tfbsRegion;
    }

    public void setTfbsRegion(String tfbsRegion) {
        this.tfbsRegion = tfbsRegion;
    }

    public Double getGenie3Weight() {
        return genie3Weight;
    }

    public void setGenie3Weight(Double genie3Weight) {
        this.genie3Weight = genie3Weight;
    }

    public Double getNes() {
        return nes;
    }

    public void setNes(Double nes) {
        this.nes = nes;
    }

    public String getPromoterTf() {
        return promoterTf;
    }

    public void setPromoterTf(String promoterTf) {
        this.promoterTf = promoterTf;
    }

    public String getSeTf() {
        return seTf;
    }

    public void setSeTf(String seTf) {
        this.seTf = seTf;
    }

    public String getTeTf() {
        return teTf;
    }

    public void setTeTf(String teTf) {
        this.teTf = teTf;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPeak() {
        return peak;
    }

    public void setPeak(String peak) {
        this.peak = peak;
    }

    public String getPeakId() {
        return peakId;
    }

    public void setPeakId(String peakId) {
        this.peakId = peakId;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public Double getLogFc() {
        return logFc;
    }

    public void setLogFc(Double logFc) {
        this.logFc = logFc;
    }

    public Double getLog2fc() {
        return log2fc;
    }

    public void setLog2fc(Double log2fc) {
        this.log2fc = log2fc;
    }

    public Double getFdr() {
        return fdr;
    }

    public void setFdr(Double fdr) {
        this.fdr = fdr;
    }

    public Double getMeanDiff() {
        return meanDiff;
    }

    public void setMeanDiff(Double meanDiff) {
        this.meanDiff = meanDiff;
    }

    public Double getAdjustedPValue() {
        return adjustedPValue;
    }

    public void setAdjustedPValue(Double adjustedPValue) {
        this.adjustedPValue = adjustedPValue;
    }
}
