package com.oscar.backend.entity;

public class CellTypeSearchRequest {
    private String cellType;
    private String tissue;

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }
}
