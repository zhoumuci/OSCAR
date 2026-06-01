package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsMarkerPeakTrackRow;
import com.oscar.backend.entity.BedtoolsOverlapRecord;
import com.oscar.backend.entity.BedtoolsP2gTrackRow;
import com.oscar.backend.entity.BedtoolsRawOverlap;
import com.oscar.backend.mapper.BedtoolsTrackMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BedtoolsResultHydrator {

    private final BedtoolsTrackMapper bedtoolsTrackMapper;

    public BedtoolsResultHydrator(BedtoolsTrackMapper bedtoolsTrackMapper) {
        this.bedtoolsTrackMapper = bedtoolsTrackMapper;
    }

    public List<BedtoolsOverlapRecord> hydrate(
            List<BedtoolsRawOverlap> rawOverlaps,
            String datasetId,
            String domain,
            String genomeBuild
    ) {
        Set<Long> markerPeakIds = new LinkedHashSet<>();
        Set<Long> p2gIds = new LinkedHashSet<>();
        for (BedtoolsRawOverlap raw : rawOverlaps) {
            if (BedtoolsAnnotationType.MARKER_PEAK.value().equals(raw.getAnnotationType())) {
                Long id = parsePrefixedId(raw.getFeatureId(), "MP:");
                if (id != null) {
                    markerPeakIds.add(id);
                }
            }
            if (BedtoolsAnnotationType.P2G_LINK.value().equals(raw.getAnnotationType())) {
                Long id = parsePrefixedId(raw.getFeatureId(), "P2G:");
                if (id != null) {
                    p2gIds.add(id);
                }
            }
        }

        Map<Long, BedtoolsMarkerPeakTrackRow> markerPeaks = markerPeakIds.isEmpty()
                ? Map.of()
                : bedtoolsTrackMapper.selectMarkerPeakHydrationRows(datasetId, domain, List.copyOf(markerPeakIds));
        Map<Long, BedtoolsP2gTrackRow> p2gLinks = p2gIds.isEmpty()
                ? Map.of()
                : bedtoolsTrackMapper.selectP2gHydrationRows(datasetId, domain, List.copyOf(p2gIds));

        return rawOverlaps.stream()
                .map(raw -> toRecord(raw, markerPeaks, p2gLinks, datasetId, domain, genomeBuild))
                .toList();
    }

    private BedtoolsOverlapRecord toRecord(
            BedtoolsRawOverlap raw,
            Map<Long, BedtoolsMarkerPeakTrackRow> markerPeaks,
            Map<Long, BedtoolsP2gTrackRow> p2gLinks,
            String datasetId,
            String domain,
            String genomeBuild
    ) {
        BedtoolsAnnotationType type = BedtoolsAnnotationType.fromValue(raw.getAnnotationType());
        BedtoolsOverlapRecord record = new BedtoolsOverlapRecord();
        record.setAnnotationType(raw.getAnnotationType());
        record.setAnnotationLabel(type == null ? raw.getAnnotationType() : type.recordLabel());
        record.setScope(raw.getScope());
        record.setFeatureId(raw.getFeatureId());
        record.setFeatureRegion(regionString(raw.getFeatureChrom(), raw.getFeatureStart(), raw.getFeatureEnd()));
        record.setOverlapBp(raw.getOverlapBp());
        record.setOverlapRatioQuery(raw.getOverlapRatioQuery());
        record.setOverlapRatioFeature(raw.getOverlapRatioFeature());

        if (type == BedtoolsAnnotationType.MARKER_PEAK) {
            hydrateMarkerPeak(record, raw, markerPeaks, datasetId, domain);
        } else if (type == BedtoolsAnnotationType.P2G_LINK) {
            hydrateP2g(record, raw, p2gLinks, datasetId, domain);
        } else {
            hydrateReference(record, raw, type, genomeBuild);
        }
        return record;
    }

    private void hydrateMarkerPeak(
            BedtoolsOverlapRecord record,
            BedtoolsRawOverlap raw,
            Map<Long, BedtoolsMarkerPeakTrackRow> markerPeaks,
            String datasetId,
            String domain
    ) {
        Long id = parsePrefixedId(raw.getFeatureId(), "MP:");
        BedtoolsMarkerPeakTrackRow row = id == null ? null : markerPeaks.get(id);
        record.setSample(datasetId + " / " + domain);
        if (row == null) {
            record.setEvidence("Peak marker");
            return;
        }
        record.setFeatureRegion(regionString(row.getChromosome(), row.getPeakStart(), row.getPeakEnd()));
        record.setGene(trimToNull(row.getLinkedGene()));
        record.setCellType(trimToNull(row.getCellType()));
        record.setCluster(trimToNull(row.getGroupName()));
        record.setCellCluster(cellCluster(row.getCellType(), row.getGroupName()));
        record.setEvidence("Peak marker: log2FC=" + display(row.getLog2fc()) + ", FDR=" + display(row.getFdr()));
    }

    private void hydrateP2g(
            BedtoolsOverlapRecord record,
            BedtoolsRawOverlap raw,
            Map<Long, BedtoolsP2gTrackRow> p2gLinks,
            String datasetId,
            String domain
    ) {
        Long id = parsePrefixedId(raw.getFeatureId(), "P2G:");
        BedtoolsP2gTrackRow row = id == null ? null : p2gLinks.get(id);
        record.setSample(datasetId + " / " + domain);
        if (row == null) {
            record.setEvidence("P2G link");
            return;
        }
        record.setFeatureRegion(regionString(row.getChromosome(), row.getPeakStart(), row.getPeakEnd()));
        record.setGene(trimToNull(row.getTargetGene()));
        record.setCellType(trimToNull(row.getCellType()));
        record.setCluster(trimToNull(row.getCluster()));
        record.setCellCluster(cellCluster(row.getCellType(), row.getCluster()));
        record.setScore(row.getP2gScore());
        record.setEvidence("P2G score=" + display(row.getP2gScore()) + "; gene/peak marker evidence available");
    }

    private void hydrateReference(
            BedtoolsOverlapRecord record,
            BedtoolsRawOverlap raw,
            BedtoolsAnnotationType type,
            String genomeBuild
    ) {
        List<String> columns = raw.getFeatureColumns();
        String name = column(columns, 3);
        String strand = firstStrand(columns);
        record.setFeatureRegion(regionString(raw.getFeatureChrom(), raw.getFeatureStart(), raw.getFeatureEnd()));
        record.setSample(genomeBuild + " reference");
        record.setStrand(strand);

        if (isRegulatoryReferenceType(type)) {
            hydrateRegulatoryReference(record, raw, type, columns, name, strand);
            return;
        }

        if (type == BedtoolsAnnotationType.GENE) {
            record.setGene(firstNonBlank(keyValue(columns, "gene_name"), keyValue(columns, "gene"), name));
            record.setEvidence("Reference gene annotation");
            return;
        }
        if (type == BedtoolsAnnotationType.TRANSCRIPT) {
            record.setTranscriptId(firstNonBlank(keyValue(columns, "transcript_id"), keyValue(columns, "transcript"), name));
            record.setGene(firstNonBlank(keyValue(columns, "gene_name"), keyValue(columns, "gene"), column(columns, 6)));
            record.setEvidence("Reference transcript annotation");
            return;
        }
        if (type == BedtoolsAnnotationType.TSS_PROMOTER) {
            record.setGene(firstNonBlank(keyValue(columns, "gene_name"), keyValue(columns, "gene"), name, column(columns, 6)));
            record.setEvidence("Reference TSS/promoter annotation");
        }
    }

    private boolean isRegulatoryReferenceType(BedtoolsAnnotationType type) {
        return type == BedtoolsAnnotationType.RISK_SNP
                || type == BedtoolsAnnotationType.COMMON_SNP
                || type == BedtoolsAnnotationType.GTEX_EQTL
                || type == BedtoolsAnnotationType.TFBS
                || type == BedtoolsAnnotationType.ENHANCER
                || type == BedtoolsAnnotationType.SUPER_ENHANCER
                || type == BedtoolsAnnotationType.METHYLATION
                || type == BedtoolsAnnotationType.CRISPR;
    }

    private void hydrateRegulatoryReference(
            BedtoolsOverlapRecord record,
            BedtoolsRawOverlap raw,
            BedtoolsAnnotationType type,
            List<String> columns,
            String name,
            String strand
    ) {
        List<String> rawFields = new ArrayList<>();
        for (int i = 6; i < columns.size(); i++) {
            String value = trimToNull(columns.get(i));
            if (value != null) {
                rawFields.add(value);
            }
        }
        if (!rawFields.isEmpty()) {
            record.setRawFields(rawFields);
        }

        if (".".equals(name) && type == BedtoolsAnnotationType.GTEX_EQTL && rawFields.size() > 2) {
            name = rawFields.get(2);
            rawFields.remove(2);
        }
        record.setName(".".equals(name) ? null : name);
        record.setStrand(strand);

        Double score = parseScore(column(columns, 4));
        record.setScore(score);

        record.setEvidence("Reference " + type.sourceLabel() + " annotation");
    }

    private Double parseScore(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parsePrefixedId(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return null;
        }
        try {
            return Long.parseLong(value.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String regionString(String chromosome, Long start, Long end) {
        String normalizedChromosome = trimToNull(chromosome);
        if (normalizedChromosome == null || start == null || end == null) {
            return null;
        }
        return normalizedChromosome + ":" + start + "-" + end;
    }

    private String cellCluster(String cellType, String cluster) {
        String normalizedCellType = trimToNull(cellType);
        String normalizedCluster = trimToNull(cluster);
        if (normalizedCellType == null) {
            return normalizedCluster;
        }
        if (normalizedCluster == null) {
            return normalizedCellType;
        }
        return normalizedCellType + " / " + normalizedCluster;
    }

    private String column(List<String> columns, int index) {
        if (columns == null || index < 0 || index >= columns.size()) {
            return null;
        }
        return trimToNull(columns.get(index));
    }

    private String firstStrand(List<String> columns) {
        if (columns == null) {
            return null;
        }
        for (String column : columns) {
            String normalized = trimToNull(column);
            if ("+".equals(normalized) || "-".equals(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String keyValue(List<String> columns, String key) {
        if (columns == null) {
            return null;
        }
        String prefix = key + "=";
        String quotedPrefix = key + " ";
        for (String column : columns) {
            String normalized = trimToNull(column);
            if (normalized == null) {
                continue;
            }
            if (normalized.startsWith(prefix)) {
                return trimToNull(normalized.substring(prefix.length()).replace("\"", "").replace(";", ""));
            }
            if (normalized.startsWith(quotedPrefix)) {
                return trimToNull(normalized.substring(quotedPrefix.length()).replace("\"", "").replace(";", ""));
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !".".equals(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private String display(Double value) {
        return value == null ? "NA" : Double.toString(value);
    }
}
