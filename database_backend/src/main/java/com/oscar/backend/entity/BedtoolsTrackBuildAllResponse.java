package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class BedtoolsTrackBuildAllResponse {

    private String domain;
    private String genomeBuild;
    private boolean force;
    private int total;
    private int built;
    private int skipped;
    private int failed;
    private List<BedtoolsTrackBuildResult> results = new ArrayList<>();

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getBuilt() {
        return built;
    }

    public void setBuilt(int built) {
        this.built = built;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<BedtoolsTrackBuildResult> getResults() {
        return results;
    }

    public void setResults(List<BedtoolsTrackBuildResult> results) {
        this.results = results;
    }
}
