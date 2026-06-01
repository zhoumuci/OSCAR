package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsSourcesResponse;

public interface BedtoolsQueryService {

    BedtoolsSourcesResponse getSources(String datasetId, String domain, String genomeBuild);

    BedtoolsIntersectResponse intersect(String datasetId, BedtoolsIntersectRequest request);
}
