package com.oscar.backend.entity;

public class BedtoolsQueryRegion {

    private String raw;
    private String chrom;
    private Long start;
    private Long end;

    public BedtoolsQueryRegion() {
    }

    public BedtoolsQueryRegion(String raw, String chrom, Long start, Long end) {
        this.raw = raw;
        this.chrom = chrom;
        this.start = start;
        this.end = end;
    }

    public long length() {
        if (start == null || end == null) {
            return 0L;
        }
        return end - start;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getChrom() {
        return chrom;
    }

    public void setChrom(String chrom) {
        this.chrom = chrom;
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
}
