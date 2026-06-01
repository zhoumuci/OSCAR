package com.oscar.backend.entity;

import java.util.List;

public class SearchResultCellTypeCompositionResponse {

    private String datasetId;
    private String groupBy;
    private List<SearchResultCellTypeItemResponse> items;

    public SearchResultCellTypeCompositionResponse() {
    }

    public SearchResultCellTypeCompositionResponse(
            String datasetId,
            String groupBy,
            List<SearchResultCellTypeItemResponse> items
    ) {
        this.datasetId = datasetId;
        this.groupBy = groupBy;
        this.items = items;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public List<SearchResultCellTypeItemResponse> getItems() {
        return items;
    }

    public void setItems(List<SearchResultCellTypeItemResponse> items) {
        this.items = items;
    }
}
