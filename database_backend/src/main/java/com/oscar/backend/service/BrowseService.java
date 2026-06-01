package com.oscar.backend.service;

import com.oscar.backend.entity.BrowseFacetResponse;
import com.oscar.backend.entity.BrowseSamplePageResponse;
import com.oscar.backend.entity.BrowseSampleQuery;

public interface BrowseService {

    BrowseSamplePageResponse getSamples(BrowseSampleQuery query);

    BrowseFacetResponse getFacets(BrowseSampleQuery query);
}
