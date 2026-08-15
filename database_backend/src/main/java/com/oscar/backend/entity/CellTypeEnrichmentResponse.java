package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class CellTypeEnrichmentResponse {

    private List<String> inputGenes = new ArrayList<>();
    private List<String> matchedGenes = new ArrayList<>();
    private List<String> unmatchedGenes = new ArrayList<>();
    private int totalResults;
    private int significantResults;
    private String topEnrichedCellType;
    private List<EnrichmentResultRow> results = new ArrayList<>();

    public List<String> getInputGenes() {
        return inputGenes;
    }

    public void setInputGenes(List<String> inputGenes) {
        this.inputGenes = inputGenes;
    }

    public List<String> getMatchedGenes() {
        return matchedGenes;
    }

    public void setMatchedGenes(List<String> matchedGenes) {
        this.matchedGenes = matchedGenes;
    }

    public List<String> getUnmatchedGenes() {
        return unmatchedGenes;
    }

    public void setUnmatchedGenes(List<String> unmatchedGenes) {
        this.unmatchedGenes = unmatchedGenes;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getSignificantResults() {
        return significantResults;
    }

    public void setSignificantResults(int significantResults) {
        this.significantResults = significantResults;
    }

    public String getTopEnrichedCellType() {
        return topEnrichedCellType;
    }

    public void setTopEnrichedCellType(String topEnrichedCellType) {
        this.topEnrichedCellType = topEnrichedCellType;
    }

    public List<EnrichmentResultRow> getResults() {
        return results;
    }

    public void setResults(List<EnrichmentResultRow> results) {
        this.results = results;
    }

    public static class EnrichmentResultRow {
        private int rank;
        private String cellType;
        private String context;
        private int overlap;
        private double enrichmentFold;
        private double pValue;
        private double fdr;
        private int datasetCount;
        private List<String> genes = new ArrayList<>();

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getCellType() {
            return cellType;
        }

        public void setCellType(String cellType) {
            this.cellType = cellType;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }

        public double getEnrichmentFold() {
            return enrichmentFold;
        }

        public void setEnrichmentFold(double enrichmentFold) {
            this.enrichmentFold = enrichmentFold;
        }

        public double getPValue() {
            return pValue;
        }

        public void setPValue(double pValue) {
            this.pValue = pValue;
        }

        public double getFdr() {
            return fdr;
        }

        public void setFdr(double fdr) {
            this.fdr = fdr;
        }

        public int getDatasetCount() {
            return datasetCount;
        }

        public void setDatasetCount(int datasetCount) {
            this.datasetCount = datasetCount;
        }

        public List<String> getGenes() {
            return genes;
        }

        public void setGenes(List<String> genes) {
            this.genes = genes;
        }
    }
}
