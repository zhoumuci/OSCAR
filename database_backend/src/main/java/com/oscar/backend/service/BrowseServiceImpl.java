package com.oscar.backend.service;

import com.oscar.backend.entity.BrowseFacetResponse;
import com.oscar.backend.entity.BrowseSamplePageResponse;
import com.oscar.backend.entity.BrowseSampleQuery;
import com.oscar.backend.entity.BrowseSampleResponse;
import com.oscar.backend.mapper.BrowseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrowseServiceImpl implements BrowseService {

    private final BrowseMapper browseMapper;

    public BrowseServiceImpl(BrowseMapper browseMapper) {
        this.browseMapper = browseMapper;
    }

    @Override
    public BrowseSamplePageResponse getSamples(BrowseSampleQuery query) {
        BrowseSampleQuery normalizedQuery = normalizeQuery(query);
        long total = browseMapper.countSamples(normalizedQuery);
        List<BrowseSampleResponse> records = total == 0
                ? List.of()
                : browseMapper.selectSamples(normalizedQuery);

        return new BrowseSamplePageResponse(
                records,
                total,
                normalizedQuery.getPage(),
                normalizedQuery.getPageSize()
        );
    }

    @Override
    public BrowseFacetResponse getFacets(BrowseSampleQuery query) {
        BrowseSampleQuery normalizedQuery = normalizeQuery(query);

        return new BrowseFacetResponse(
                browseMapper.selectSpeciesFacets(normalizedQuery),
                browseMapper.selectSampleTypeFacets(normalizedQuery),
                browseMapper.selectTissueFacets(normalizedQuery)
        );
    }

    private BrowseSampleQuery normalizeQuery(BrowseSampleQuery query) {
        BrowseSampleQuery normalizedQuery = query == null ? new BrowseSampleQuery() : query;
        normalizedQuery.normalize();
        return normalizedQuery;
    }
}
