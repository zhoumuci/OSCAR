package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisMapperSqlTest {

    @Test
    void sequenceEvidenceQueriesUseBedHalfOpenOverlapSemantics() throws Exception {
        Map<String, Object> parameters = Map.of(
                "chromosome", "chr1",
                "regionStart", 99L,
                "regionEnd", 200L,
                "datasetId", "",
                "limit", 100
        );

        for (String methodName : List.of("selectP2gLinksByRegion", "selectMarkerPeaksByRegion")) {
            String sql = renderSelect(
                    AnalysisMapper.class.getMethod(
                            methodName,
                            String.class,
                            long.class,
                            long.class,
                            String.class,
                            int.class
                    ),
                    parameters
            );

            assertTrue(sql.contains("peak_start < ?"));
            assertTrue(sql.contains("peak_end > ?"));
            assertFalse(sql.contains("peak_start <= ?"));
            assertFalse(sql.contains("peak_end >= ?"));
        }
    }

    @Test
    void markerPeakEvidenceUsesMarkerRowsAsTheBaseAndExactIndexedP2gJoin() throws Exception {
        String sql = renderSequenceEvidenceQuery("selectMarkerPeaksByRegion", 100);

        assertTrue(sql.contains("WITH selected_marker_peaks AS"));
        assertTrue(sql.contains("FROM oscar_marker_peak"));
        assertTrue(sql.contains("LEFT JOIN oscar_peak_gene_link pgl"));
        assertTrue(sql.contains("pgl.dataset_id = mp.dataset_id"));
        assertTrue(sql.contains("pgl.domain = mp.domain"));
        assertTrue(sql.contains("pgl.chromosome = mp.chromosome"));
        assertTrue(sql.contains("pgl.peak_start = mp.peak_start"));
        assertTrue(sql.contains("pgl.peak_end = mp.peak_end"));
        assertTrue(sql.contains("pgl.is_deleted = 0"));
        assertTrue(sql.contains("pgl.is_visible = 1"));
    }

    @Test
    void sequenceEvidenceLimitIsOnlyRenderedWhenTheUserProvidesIt() throws Exception {
        for (String methodName : List.of("selectP2gLinksByRegion", "selectMarkerPeaksByRegion")) {
            assertFalse(renderSequenceEvidenceQuery(methodName, 0).contains("LIMIT"));
            assertTrue(renderSequenceEvidenceQuery(methodName, 100).contains("LIMIT ?"));
        }
    }

    @Test
    void rendersCompleteHypothesisCountForEveryActiveResultLevel() throws Exception {
        for (String resultLevel : List.of("cell_type", "dataset_cell_type", "cluster")) {
            String sql = renderSelect(
                    AnalysisMapper.class.getMethod(
                            "countEnrichmentMarkerSets",
                            String.class,
                            String.class,
                            List.class,
                            String.class
                    ),
                    Map.of(
                            "resultLevel", resultLevel,
                            "domain", "integration",
                            "signalTypes", List.of("gene_expression", "gene_exp"),
                            "datasetId", "cluster".equals(resultLevel) ? "H_000001" : ""
                    )
            );

            assertFalse(sql.contains("celltype_standard"));
            assertTrue(sql.contains("COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')"));
            assertTrue(sql.contains("GROUP BY"));
        }
    }

    @Test
    void rendersStoredDatasetCellTypeCandidateQuery() throws Exception {
        String sql = renderSelect(
                AnalysisMapper.class.getMethod(
                        "selectDatasetCellTypeCandidateEnrichmentMarkerSets",
                        List.class,
                        String.class,
                        List.class
                ),
                Map.of(
                        "genes", List.of("CD3D", "CD3E"),
                        "domain", "integration",
                        "signalTypes", List.of("gene_expression", "gene_exp")
                )
        );

        assertTrue(sql.contains("COALESCE(NULLIF(TRIM(candidate.major_cell_type), ''), 'Unknown')"));
        assertFalse(sql.contains("celltype_standard"));
        assertTrue(sql.contains("COUNT(DISTINCT gms.gene_symbol)"));
    }

    @Test
    void rendersSingleDatasetClusterUsingGroupNameAsContext() throws Exception {
        String sql = renderSelect(
                AnalysisMapper.class.getMethod(
                        "selectEnrichmentOverlapGenes",
                        List.class,
                        String.class,
                        String.class,
                        List.class,
                        String.class
                ),
                Map.of(
                        "genes", List.of("CD3D", "CD3E"),
                        "resultLevel", "cluster",
                        "domain", "integration",
                        "signalTypes", List.of("gene_expression", "gene_exp"),
                        "datasetId", "H_000001"
                )
        );

        String clusterContext = "COALESCE(NULLIF(TRIM(gms.group_name), ''), 'Unknown')";
        assertTrue(sql.contains(clusterContext + " AS context"));
        assertTrue(sql.indexOf(clusterContext) != sql.lastIndexOf(clusterContext));
        assertFalse(sql.contains("display_context"));
    }

    @Test
    void rendersStoredCellTypeGlobalPrecomputationQuery() throws Exception {
        Method method = RegulatoryAnnotationMaintenanceMapper.class.getMethod(
                "upsertGlobalCellTypeEnrichmentSets",
                String.class,
                String.class,
                List.class
        );
        Insert insert = method.getAnnotation(Insert.class);
        String sql = render(String.join(" ", insert.value()), Map.of(
                "markerReference", "integration_expression",
                "domain", "integration",
                "signalTypes", List.of("gene_expression", "gene_exp")
        ));

        assertFalse(sql.contains("celltype_standard"));
        assertTrue(sql.contains("COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')"));
        assertTrue(sql.contains("COUNT(DISTINCT gms.gene_symbol)"));
        assertTrue(sql.contains("COUNT(DISTINCT gms.dataset_id)"));
    }

    @Test
    void cellTypeSearchUsesStoredCellTypeDirectly() throws Exception {
        String sql = renderSelect(
                SearchMapper.class.getMethod(
                        "findSamplesByCellType",
                        List.class,
                        boolean.class,
                        int.class
                ),
                Map.of(
                        "cellTypes", List.of("B cells", "T cells"),
                        "intersection", true,
                        "minTypes", 2
                )
        );

        assertTrue(sql.contains("WHERE ca.major_cell_type IN"));
        assertTrue(sql.contains("COUNT(DISTINCT NULLIF(ca.major_cell_type, '')) >= ?"));
        assertFalse(sql.contains(")) GROUP BY"));
        assertFalse(sql.contains("celltype_standard"));
    }

    @Test
    void runtimeMapperQueriesDoNotReadCellTypeStandard() {
        for (Class<?> mapperClass : List.of(
                AnalysisMapper.class,
                BedtoolsTrackMapper.class,
                DownloadMapper.class,
                FeatureOccurrenceMapper.class,
                PeakGeneContextMapper.class,
                RegulatoryAnnotationMaintenanceMapper.class,
                RegulatoryAnnotationMapper.class,
                SearchMapper.class,
                SearchResultMapper.class
        )) {
            for (Method method : mapperClass.getDeclaredMethods()) {
                Select select = method.getAnnotation(Select.class);
                if (select != null) {
                    assertFalse(
                            String.join(" ", select.value()).contains("celltype_standard"),
                            mapperClass.getSimpleName() + "." + method.getName()
                    );
                }
                Insert insert = method.getAnnotation(Insert.class);
                if (insert != null) {
                    assertFalse(
                            String.join(" ", insert.value()).contains("celltype_standard"),
                            mapperClass.getSimpleName() + "." + method.getName()
                    );
                }
            }
        }
    }

    private static String renderSelect(Method method, Map<String, Object> parameters) {
        Select select = method.getAnnotation(Select.class);
        return render(String.join(" ", select.value()), parameters);
    }

    private static String renderSequenceEvidenceQuery(String methodName, int limit) throws Exception {
        return renderSelect(
                AnalysisMapper.class.getMethod(
                        methodName,
                        String.class,
                        long.class,
                        long.class,
                        String.class,
                        int.class
                ),
                Map.of(
                        "chromosome", "chr1",
                        "regionStart", 99L,
                        "regionEnd", 200L,
                        "datasetId", "H_000001",
                        "limit", limit
                )
        );
    }

    private static String render(String script, Map<String, Object> parameters) {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(configuration, script, Map.class);
        BoundSql boundSql = sqlSource.getBoundSql(parameters);
        return SqlSourceBuilder.removeExtraWhitespaces(boundSql.getSql());
    }
}
