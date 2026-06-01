package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class ReferenceTrackRefreshResponse {

    private String genomeBuild;
    private String category;
    private String manifestPath;
    private int refreshed;
    private int ready;
    private int missing;
    private int error;
    private List<ReferenceTrackDto> tracks = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public void setManifestPath(String manifestPath) {
        this.manifestPath = manifestPath;
    }

    public int getRefreshed() {
        return refreshed;
    }

    public void setRefreshed(int refreshed) {
        this.refreshed = refreshed;
    }

    public int getReady() {
        return ready;
    }

    public void setReady(int ready) {
        this.ready = ready;
    }

    public int getMissing() {
        return missing;
    }

    public void setMissing(int missing) {
        this.missing = missing;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public List<ReferenceTrackDto> getTracks() {
        return tracks;
    }

    public void setTracks(List<ReferenceTrackDto> tracks) {
        this.tracks = tracks;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
