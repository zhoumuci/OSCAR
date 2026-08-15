package com.oscar.backend.entity;

import com.oscar.backend.entity.SequencePeak2GeneResponse.MarkerPeakDto;
import com.oscar.backend.entity.SequencePeak2GeneResponse.PeakGeneLinkDto;

import java.util.List;

public class SequencePeak2GeneEvidenceResponse {

    private String hitId;
    private int hitRank;
    private String chromosome;
    private long start;
    private long end;
    private long bedStart;
    private long bedEnd;
    private String coordinateSystem = "BED 0-based half-open";
    private int overlappingPeakCount;
    private int linkedGeneCount;
    private int returnedP2gCount;
    private int returnedMarkerPeakCount;
    private boolean possiblyTruncated;
    private List<PeakGeneLinkDto> peakGeneLinks;
    private List<MarkerPeakDto> markerPeaks;

    public String getHitId() { return hitId; }
    public void setHitId(String v) { this.hitId = v; }
    public int getHitRank() { return hitRank; }
    public void setHitRank(int v) { this.hitRank = v; }
    public String getChromosome() { return chromosome; }
    public void setChromosome(String v) { this.chromosome = v; }
    public long getStart() { return start; }
    public void setStart(long v) { this.start = v; }
    public long getEnd() { return end; }
    public void setEnd(long v) { this.end = v; }
    public long getBedStart() { return bedStart; }
    public void setBedStart(long v) { this.bedStart = v; }
    public long getBedEnd() { return bedEnd; }
    public void setBedEnd(long v) { this.bedEnd = v; }
    public String getCoordinateSystem() { return coordinateSystem; }
    public void setCoordinateSystem(String v) { this.coordinateSystem = v; }
    public int getOverlappingPeakCount() { return overlappingPeakCount; }
    public void setOverlappingPeakCount(int v) { this.overlappingPeakCount = v; }
    public int getLinkedGeneCount() { return linkedGeneCount; }
    public void setLinkedGeneCount(int v) { this.linkedGeneCount = v; }
    public int getReturnedP2gCount() { return returnedP2gCount; }
    public void setReturnedP2gCount(int v) { this.returnedP2gCount = v; }
    public int getReturnedMarkerPeakCount() { return returnedMarkerPeakCount; }
    public void setReturnedMarkerPeakCount(int v) { this.returnedMarkerPeakCount = v; }
    public boolean isPossiblyTruncated() { return possiblyTruncated; }
    public void setPossiblyTruncated(boolean v) { this.possiblyTruncated = v; }
    public List<PeakGeneLinkDto> getPeakGeneLinks() { return peakGeneLinks; }
    public void setPeakGeneLinks(List<PeakGeneLinkDto> v) { this.peakGeneLinks = v; }
    public List<MarkerPeakDto> getMarkerPeaks() { return markerPeaks; }
    public void setMarkerPeaks(List<MarkerPeakDto> v) { this.markerPeaks = v; }
}
