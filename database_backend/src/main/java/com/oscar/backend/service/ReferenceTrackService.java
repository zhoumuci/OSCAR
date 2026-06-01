package com.oscar.backend.service;

import com.oscar.backend.entity.ReferenceTrackRefreshResponse;
import com.oscar.backend.entity.ReferenceTrackStatusResponse;

public interface ReferenceTrackService {

    ReferenceTrackRefreshResponse refreshReferenceTracks(String genomeBuild, String category);

    ReferenceTrackStatusResponse getReferenceTrackStatus(String genomeBuild, String category);
}
