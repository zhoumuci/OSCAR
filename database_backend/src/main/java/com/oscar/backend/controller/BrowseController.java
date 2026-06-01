package com.oscar.backend.controller;

import com.oscar.backend.entity.BrowseFacetResponse;
import com.oscar.backend.entity.BrowseSamplePageResponse;
import com.oscar.backend.entity.BrowseSampleQuery;
import com.oscar.backend.service.BrowseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/browse")
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/samples")
    public BrowseSamplePageResponse getSamples(@ModelAttribute BrowseSampleQuery query) {
        return browseService.getSamples(query);
    }

    @GetMapping("/facets")
    public BrowseFacetResponse getFacets(@ModelAttribute BrowseSampleQuery query) {
        return browseService.getFacets(query);
    }
}
