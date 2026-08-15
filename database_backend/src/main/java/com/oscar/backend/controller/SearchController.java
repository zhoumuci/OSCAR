package com.oscar.backend.controller;

import com.oscar.backend.entity.GeneSearchRequest;
import com.oscar.backend.entity.GeneSearchResponse;
import com.oscar.backend.entity.CellTypeSearchRequest;
import com.oscar.backend.entity.PeakSearchRequest;
import com.oscar.backend.entity.PeakSearchResponse;
import com.oscar.backend.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/gene")
    public GeneSearchResponse searchByGene(@RequestBody GeneSearchRequest request) {
        return searchService.searchByGene(request);
    }

    @PostMapping("/peak")
    public PeakSearchResponse searchByPeak(@RequestBody PeakSearchRequest request) {
        return searchService.searchByPeak(request);
    }

    @PostMapping("/tissue")
    public GeneSearchResponse searchByTissue(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tissues = (List<String>) body.get("tissues");
        return searchService.searchByTissue(tissues);
    }

    @PostMapping("/cell-type")
    public GeneSearchResponse searchByCellType(@RequestBody CellTypeSearchRequest request) {
        return searchService.searchByCellType(request.getCellType());
    }

    @GetMapping("/cell-types")
    public List<String> listCellTypes() {
        return searchService.listCellTypes();
    }

    @GetMapping("/tissue-counts")
    public List<Map<String, Object>> tissueCounts() {
        return searchService.tissueCounts();
    }

    @GetMapping("/cell-type-counts")
    public List<Map<String, Object>> cellTypeCounts() {
        return searchService.cellTypeCounts();
    }
}
