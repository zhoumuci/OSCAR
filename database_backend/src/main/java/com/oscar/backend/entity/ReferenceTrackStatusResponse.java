package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class ReferenceTrackStatusResponse {

    private String genomeBuild;
    private String category;
    private List<ReferenceTrackDto> tracks = new ArrayList<>();

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

    public List<ReferenceTrackDto> getTracks() {
        return tracks;
    }

    public void setTracks(List<ReferenceTrackDto> tracks) {
        this.tracks = tracks;
    }
}
