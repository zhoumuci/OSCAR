package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegulatoryAnnotationMapperSqlTest {

    @Test
    void markerPeakStreamRanksDatasetLinksOnceAndUsesIndexedGeneFilter() throws Exception {
        Method method = RegulatoryAnnotationMapper.class.getMethod(
                "streamMarkerPeaks",
                String.class, String.class, String.class, String.class, String.class,
                Long.class, Long.class, String.class, String.class, Double.class,
                Double.class, String.class
        );
        Select select = method.getAnnotation(Select.class);
        String sql = render(String.join(" ", select.value()), Map.ofEntries(
                Map.entry("datasetId", "H_000001"),
                Map.entry("domain", "integration"),
                Map.entry("targetGene", "CD22"),
                Map.entry("peakExact", "chr1:100-200"),
                Map.entry("peakChromosome", "chr1"),
                Map.entry("peakStart", 100L),
                Map.entry("peakEnd", 200L),
                Map.entry("contextCellType", "B cell"),
                Map.entry("contextCluster", "1"),
                Map.entry("maxFdr", 0.05D),
                Map.entry("minLog2fc", 0.25D),
                Map.entry("orderBy", "mp.id ASC")
        ));

        assertTrue(sql.contains("WITH ranked_peak_links AS"));
        assertTrue(sql.contains("FROM oscar_peak_gene_link pgl"));
        assertTrue(sql.contains("pgl.dataset_id = ?"));
        assertTrue(sql.contains("pgl.domain = ?"));
        assertTrue(sql.contains("pgl.gene_name = ?"));
        assertTrue(sql.contains("pgl.chromosome = ?"));
        assertTrue(sql.contains("pgl.peak_start < ?"));
        assertTrue(sql.contains("pgl.peak_end > ?"));
        assertTrue(sql.contains("ROW_NUMBER() OVER"));
        assertTrue(sql.contains("PARTITION BY pgl.dataset_id, pgl.domain, pgl.chromosome, pgl.peak_start, pgl.peak_end"));
        assertTrue(sql.contains("LEFT JOIN ranked_peak_links best_link"));
        assertTrue(sql.contains("best_link.linkRank = 1"));
        assertTrue(sql.contains("best_link.linkedGeneName IS NOT NULL"));
        assertFalse(sql.contains("FROM oscar_peak_gene_link pgl_display"));
        assertFalse(sql.contains("WITH requested_peaks"));
        assertFalse(sql.contains("LIMIT ? OFFSET ?"));
    }

    private static String render(String script, Map<String, Object> parameters) {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(configuration, script, Map.class);
        BoundSql boundSql = sqlSource.getBoundSql(parameters);
        return SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql());
    }
}
