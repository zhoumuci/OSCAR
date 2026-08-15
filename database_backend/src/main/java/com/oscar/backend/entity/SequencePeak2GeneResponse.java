package com.oscar.backend.entity;

import java.util.List;

public class SequencePeak2GeneResponse {

    private QueryInfo query;
    private SummaryInfo summary;
    private String mappingStatus;
    private String mappingMessage;
    private String evidenceHitId;
    private List<BlastHitDto> blastHits;
    private List<PeakGeneLinkDto> peakGeneLinks;
    private List<MarkerPeakDto> markerPeaks;

    public QueryInfo getQuery() { return query; }
    public void setQuery(QueryInfo v) { this.query = v; }

    public SummaryInfo getSummary() { return summary; }
    public void setSummary(SummaryInfo v) { this.summary = v; }

    public String getMappingStatus() { return mappingStatus; }
    public void setMappingStatus(String v) { this.mappingStatus = v; }

    public String getMappingMessage() { return mappingMessage; }
    public void setMappingMessage(String v) { this.mappingMessage = v; }

    public String getEvidenceHitId() { return evidenceHitId; }
    public void setEvidenceHitId(String v) { this.evidenceHitId = v; }

    public List<BlastHitDto> getBlastHits() { return blastHits; }
    public void setBlastHits(List<BlastHitDto> v) { this.blastHits = v; }

    public List<PeakGeneLinkDto> getPeakGeneLinks() { return peakGeneLinks; }
    public void setPeakGeneLinks(List<PeakGeneLinkDto> v) { this.peakGeneLinks = v; }

    public List<MarkerPeakDto> getMarkerPeaks() { return markerPeaks; }
    public void setMarkerPeaks(List<MarkerPeakDto> v) { this.markerPeaks = v; }

    // ---- inner classes ----

    public static class QueryInfo {
        private int sequenceLength;
        private String genomeBuild;
        private int usedHitIndex;
        private double nearEquivalentScoreRatio;
        private String blastCoordinateSystem;
        private String evidenceCoordinateSystem;
        public int getSequenceLength() { return sequenceLength; }
        public void setSequenceLength(int v) { this.sequenceLength = v; }
        public String getGenomeBuild() { return genomeBuild; }
        public void setGenomeBuild(String v) { this.genomeBuild = v; }
        public int getUsedHitIndex() { return usedHitIndex; }
        public void setUsedHitIndex(int v) { this.usedHitIndex = v; }
        public double getNearEquivalentScoreRatio() { return nearEquivalentScoreRatio; }
        public void setNearEquivalentScoreRatio(double v) { this.nearEquivalentScoreRatio = v; }
        public String getBlastCoordinateSystem() { return blastCoordinateSystem; }
        public void setBlastCoordinateSystem(String v) { this.blastCoordinateSystem = v; }
        public String getEvidenceCoordinateSystem() { return evidenceCoordinateSystem; }
        public void setEvidenceCoordinateSystem(String v) { this.evidenceCoordinateSystem = v; }
    }

    public static class SummaryInfo {
        private int blastHitCount;
        private int returnedBlastHitCount;
        private boolean blastHitsTruncated;
        private int mappedRegionCount;
        private int candidateLocusCount;
        private int qualifiedCandidateCount;
        private int nearEquivalentLocusCount;
        private int subjectCount;
        private boolean candidateSearchLimited;
        private int overlappingPeakCount;
        private int linkedGeneCount;
        private int markerPeakCount;
        private int returnedP2gCount;
        private int returnedMarkerPeakCount;
        private boolean evidencePossiblyTruncated;
        public int getBlastHitCount() { return blastHitCount; }
        public void setBlastHitCount(int v) { this.blastHitCount = v; }
        public int getReturnedBlastHitCount() { return returnedBlastHitCount; }
        public void setReturnedBlastHitCount(int v) { this.returnedBlastHitCount = v; }
        public boolean isBlastHitsTruncated() { return blastHitsTruncated; }
        public void setBlastHitsTruncated(boolean v) { this.blastHitsTruncated = v; }
        public int getMappedRegionCount() { return mappedRegionCount; }
        public void setMappedRegionCount(int v) { this.mappedRegionCount = v; }
        public int getCandidateLocusCount() { return candidateLocusCount; }
        public void setCandidateLocusCount(int v) { this.candidateLocusCount = v; }
        public int getQualifiedCandidateCount() { return qualifiedCandidateCount; }
        public void setQualifiedCandidateCount(int v) { this.qualifiedCandidateCount = v; }
        public int getNearEquivalentLocusCount() { return nearEquivalentLocusCount; }
        public void setNearEquivalentLocusCount(int v) { this.nearEquivalentLocusCount = v; }
        public int getSubjectCount() { return subjectCount; }
        public void setSubjectCount(int v) { this.subjectCount = v; }
        public boolean isCandidateSearchLimited() { return candidateSearchLimited; }
        public void setCandidateSearchLimited(boolean v) { this.candidateSearchLimited = v; }
        public int getOverlappingPeakCount() { return overlappingPeakCount; }
        public void setOverlappingPeakCount(int v) { this.overlappingPeakCount = v; }
        public int getLinkedGeneCount() { return linkedGeneCount; }
        public void setLinkedGeneCount(int v) { this.linkedGeneCount = v; }
        public int getMarkerPeakCount() { return markerPeakCount; }
        public void setMarkerPeakCount(int v) { this.markerPeakCount = v; }
        public int getReturnedP2gCount() { return returnedP2gCount; }
        public void setReturnedP2gCount(int v) { this.returnedP2gCount = v; }
        public int getReturnedMarkerPeakCount() { return returnedMarkerPeakCount; }
        public void setReturnedMarkerPeakCount(int v) { this.returnedMarkerPeakCount = v; }
        public boolean isEvidencePossiblyTruncated() { return evidencePossiblyTruncated; }
        public void setEvidencePossiblyTruncated(boolean v) { this.evidencePossiblyTruncated = v; }
    }

    public static class BlastHitDto {
        private int rank;
        private String hitId;
        private String chromosome;
        private long start;
        private long end;
        private long bedStart;
        private long bedEnd;
        private String strand;
        private double identity;
        private double queryCoverage;
        private double scoreRatio;
        private boolean primary;
        private boolean nearEquivalent;
        private int alignLen;
        private int mismatch;
        private int gapOpen;
        private long qStart;
        private long qEnd;
        private String evalue;
        private double bitScore;
        public int getRank() { return rank; } public void setRank(int v) { this.rank = v; }
        public String getHitId() { return hitId; } public void setHitId(String v) { this.hitId = v; }
        public String getChromosome() { return chromosome; } public void setChromosome(String v) { this.chromosome = v; }
        public long getStart() { return start; } public void setStart(long v) { this.start = v; }
        public long getEnd() { return end; } public void setEnd(long v) { this.end = v; }
        public long getBedStart() { return bedStart; } public void setBedStart(long v) { this.bedStart = v; }
        public long getBedEnd() { return bedEnd; } public void setBedEnd(long v) { this.bedEnd = v; }
        public String getStrand() { return strand; } public void setStrand(String v) { this.strand = v; }
        public double getIdentity() { return identity; } public void setIdentity(double v) { this.identity = v; }
        public double getQueryCoverage() { return queryCoverage; } public void setQueryCoverage(double v) { this.queryCoverage = v; }
        public double getScoreRatio() { return scoreRatio; } public void setScoreRatio(double v) { this.scoreRatio = v; }
        public boolean isPrimary() { return primary; } public void setPrimary(boolean v) { this.primary = v; }
        public boolean isNearEquivalent() { return nearEquivalent; } public void setNearEquivalent(boolean v) { this.nearEquivalent = v; }
        public int getAlignLen() { return alignLen; } public void setAlignLen(int v) { this.alignLen = v; }
        public int getMismatch() { return mismatch; } public void setMismatch(int v) { this.mismatch = v; }
        public int getGapOpen() { return gapOpen; } public void setGapOpen(int v) { this.gapOpen = v; }
        public long getQStart() { return qStart; } public void setQStart(long v) { this.qStart = v; }
        public long getQEnd() { return qEnd; } public void setQEnd(long v) { this.qEnd = v; }
        public String getEvalue() { return evalue; } public void setEvalue(String v) { this.evalue = v; }
        public double getBitScore() { return bitScore; } public void setBitScore(double v) { this.bitScore = v; }
    }

    public static class PeakGeneLinkDto {
        private String datasetId;
        private String domain;
        private String peakName;
        private String chromosome;
        private long peakStart;
        private long peakEnd;
        private String geneName;
        private Double correlation;
        private Double fdr;
        private Double linkScore;
        private String sourceFile;
        public String getDatasetId() { return datasetId; } public void setDatasetId(String v) { this.datasetId = v; }
        public String getDomain() { return domain; } public void setDomain(String v) { this.domain = v; }
        public String getPeakName() { return peakName; } public void setPeakName(String v) { this.peakName = v; }
        public String getChromosome() { return chromosome; } public void setChromosome(String v) { this.chromosome = v; }
        public long getPeakStart() { return peakStart; } public void setPeakStart(long v) { this.peakStart = v; }
        public long getPeakEnd() { return peakEnd; } public void setPeakEnd(long v) { this.peakEnd = v; }
        public String getGeneName() { return geneName; } public void setGeneName(String v) { this.geneName = v; }
        public Double getCorrelation() { return correlation; } public void setCorrelation(Double v) { this.correlation = v; }
        public Double getFdr() { return fdr; } public void setFdr(Double v) { this.fdr = v; }
        public Double getLinkScore() { return linkScore; } public void setLinkScore(Double v) { this.linkScore = v; }
        public String getSourceFile() { return sourceFile; } public void setSourceFile(String v) { this.sourceFile = v; }
    }

    public static class MarkerPeakDto {
        private String datasetId;
        private String domain;
        private String clusterSource;
        private String groupName;
        private String peakName;
        private String chromosome;
        private long peakStart;
        private long peakEnd;
        private Double log2fc;
        private Double fdr;
        private Double meanDiff;
        private String sourceFile;
        private List<PeakGeneLinkDto> peakGeneLinks;
        public String getDatasetId() { return datasetId; } public void setDatasetId(String v) { this.datasetId = v; }
        public String getDomain() { return domain; } public void setDomain(String v) { this.domain = v; }
        public String getClusterSource() { return clusterSource; } public void setClusterSource(String v) { this.clusterSource = v; }
        public String getGroupName() { return groupName; } public void setGroupName(String v) { this.groupName = v; }
        public String getPeakName() { return peakName; } public void setPeakName(String v) { this.peakName = v; }
        public String getChromosome() { return chromosome; } public void setChromosome(String v) { this.chromosome = v; }
        public long getPeakStart() { return peakStart; } public void setPeakStart(long v) { this.peakStart = v; }
        public long getPeakEnd() { return peakEnd; } public void setPeakEnd(long v) { this.peakEnd = v; }
        public Double getLog2fc() { return log2fc; } public void setLog2fc(Double v) { this.log2fc = v; }
        public Double getFdr() { return fdr; } public void setFdr(Double v) { this.fdr = v; }
        public Double getMeanDiff() { return meanDiff; } public void setMeanDiff(Double v) { this.meanDiff = v; }
        public String getSourceFile() { return sourceFile; } public void setSourceFile(String v) { this.sourceFile = v; }
        public List<PeakGeneLinkDto> getPeakGeneLinks() { return peakGeneLinks; } public void setPeakGeneLinks(List<PeakGeneLinkDto> v) { this.peakGeneLinks = v; }
    }
}
