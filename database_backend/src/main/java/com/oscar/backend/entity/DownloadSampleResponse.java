package com.oscar.backend.entity;

import java.util.List;

public class DownloadSampleResponse {

    private int total;
    private List<DownloadSampleItem> items;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<DownloadSampleItem> getItems() { return items; }
    public void setItems(List<DownloadSampleItem> items) { this.items = items; }

    public static class DownloadSampleItem {
        private String datasetId;
        private String sampleName;
        private String sampleType;
        private String tissue;
        private String disease;
        private String sampleSource;
        private Integer cellCount;
        private String platform;
        private String sourceId;

        public String getDatasetId() { return datasetId; }
        public void setDatasetId(String v) { this.datasetId = v; }
        public String getSampleName() { return sampleName; }
        public void setSampleName(String v) { this.sampleName = v; }
        public String getSampleType() { return sampleType; }
        public void setSampleType(String v) { this.sampleType = v; }
        public String getTissue() { return tissue; }
        public void setTissue(String v) { this.tissue = v; }
        public String getDisease() { return disease; }
        public void setDisease(String v) { this.disease = v; }
        public String getSampleSource() { return sampleSource; }
        public void setSampleSource(String v) { this.sampleSource = v; }
        public Integer getCellCount() { return cellCount; }
        public void setCellCount(Integer v) { this.cellCount = v; }
        public String getPlatform() { return platform; }
        public void setPlatform(String v) { this.platform = v; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String v) { this.sourceId = v; }
    }
}
