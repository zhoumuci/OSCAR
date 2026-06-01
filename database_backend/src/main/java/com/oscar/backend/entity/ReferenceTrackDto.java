package com.oscar.backend.entity;

import java.time.LocalDateTime;

public class ReferenceTrackDto {

    private String genomeBuild;
    private String category;
    private String sourceType;
    private String label;
    private String status;
    private String filePath;
    private String fileFormat;
    private String coordinateSystem;
    private String coordinateMode;
    private Long rowCount;
    private Long fileSizeBytes;
    private String md5;
    private String description;
    private String manifestPath;
    private Integer displayOrder;
    private Integer warningCount;
    private LocalDateTime updatedAt;

    public static ReferenceTrackDto fromTrack(ReferenceTrack track) {
        ReferenceTrackDto dto = new ReferenceTrackDto();
        dto.setGenomeBuild(track.getGenomeBuild());
        dto.setCategory(track.getCategory());
        dto.setSourceType(track.getSourceType());
        dto.setLabel(track.getLabel());
        dto.setStatus(track.getStatus());
        dto.setFilePath(track.getFilePath());
        dto.setFileFormat(track.getFileFormat());
        dto.setCoordinateSystem(track.getCoordinateSystem());
        dto.setCoordinateMode(track.getCoordinateMode());
        dto.setRowCount(track.getRowCount());
        dto.setFileSizeBytes(track.getFileSizeBytes());
        dto.setMd5(track.getMd5());
        dto.setDescription(track.getDescription());
        dto.setManifestPath(track.getManifestPath());
        dto.setDisplayOrder(track.getDisplayOrder());
        dto.setUpdatedAt(track.getUpdatedAt());
        return dto;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    public void setCoordinateSystem(String coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    public String getCoordinateMode() {
        return coordinateMode;
    }

    public void setCoordinateMode(String coordinateMode) {
        this.coordinateMode = coordinateMode;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public void setManifestPath(String manifestPath) {
        this.manifestPath = manifestPath;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
