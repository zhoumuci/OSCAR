package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsSourcesResponse;

import java.util.List;

public interface BedtoolsQueryService {

    BedtoolsSourcesResponse getSources(String datasetId, String domain, String genomeBuild);

    BedtoolsIntersectResponse intersect(String datasetId, BedtoolsIntersectRequest request);

    BedtoolsIntersectResponse referenceIntersect(String genomeBuild, BedtoolsIntersectRequest request);

    BedtoolsIntersectResponse referenceIntersectAll(String genomeBuild, BedtoolsIntersectRequest request);

    BedtoolsIntersectResponse referenceIntersectAll(
            String genomeBuild,
            List<ReferenceQueryRegion> queryRegions,
            String annotationType
    );

    record ReferenceQueryRegion(String id, String region) {}
}
