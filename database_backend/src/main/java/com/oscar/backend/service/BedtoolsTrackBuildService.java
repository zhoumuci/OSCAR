package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsTrackBuildAllResponse;
import com.oscar.backend.entity.BedtoolsTrackStatusResponse;

public interface BedtoolsTrackBuildService {

    BedtoolsTrackStatusResponse getTrackStatus(String datasetId, String domain, String genomeBuild);

    BedtoolsTrackStatusResponse buildSampleTracks(String datasetId, String domain, String genomeBuild, boolean force);

    BedtoolsTrackBuildAllResponse buildAllSampleTracks(String domain, String genomeBuild, boolean force);
}
