package com.oscar.backend.mapper;

import com.oscar.backend.entity.SearchResultCellTypeItemResponse;
import com.oscar.backend.entity.SearchResultOverviewResponse;
import com.oscar.backend.entity.SearchResultQcSummaryRow;
import com.oscar.backend.entity.SearchResultQcValueRow;
import com.oscar.backend.entity.SearchResultUmapPointResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchResultMapper {

    String CELL_VISIBLE_FILTER = """
            c.dataset_id = #{datasetId}
            AND (c.is_deleted IS NULL OR c.is_deleted = 0)
            AND (c.is_visible IS NULL OR c.is_visible = 1)
            AND EXISTS (
                SELECT 1
                FROM oscar_sample s
                WHERE s.dataset_id = c.dataset_id
                  AND (s.is_deleted IS NULL OR s.is_deleted = 0)
                  AND (s.is_visible IS NULL OR s.is_visible = 1)
            )
            """;

    /* Shared JOIN to oscar_cluster_annotation for domain-aware cluster labels.
     * clusterRaw is a bare column reference (e.g. c.cluster_wnn), safe for ${}.
     * domain is a bound param (#{}), safe from SQL injection. */
    String CLUSTER_ANNOTATION_JOIN = """
            LEFT JOIN oscar_cluster_annotation ca
              ON #{domain} = 'integration'
             AND ca.dataset_id = c.dataset_id
             AND ca.domain = #{domain}
             AND ca.cluster_label = ${clusterRaw}
            """;

    /* Integration may display its WNN cell type together with the WNN cluster.
     * RNA and ATAC have no independent cell-type annotation, so their labels
     * must contain only the modality-specific cluster. */
    String DOMAIN_CLUSTER_LABEL_EXPR = """
            CASE
                WHEN #{domain} = 'integration' THEN CONCAT(
                    COALESCE(NULLIF(TRIM(ca.major_cell_type), ''), 'Unannotated'),
                    ' / ',
                    COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown')
                )
                ELSE COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown')
            END
            """;

    @Select("""
            SELECT COUNT(*)
            FROM oscar_sample
            WHERE dataset_id = #{datasetId}
              AND (is_deleted IS NULL OR is_deleted = 0)
              AND (is_visible IS NULL OR is_visible = 1)
            """)
    long countVisibleSampleByDatasetId(@Param("datasetId") String datasetId);

    @Select("""
            SELECT
                s.dataset_id AS datasetId,
                s.assay_type AS domain,
                s.tissue,
                s.disease,
                CAST(COALESCE(s.cell_count, ss.total_cells) AS SIGNED) AS sampleNumber,
                CAST(NULL AS CHAR) AS pmid,
                CAST(NULL AS CHAR) AS downloadUrl,
                s.species,
                s.sample_type AS sampleType,
                s.sample_name AS sampleName,
                s.platform,
                s.source_id AS sourceId,
                s.sample_source AS sampleSource,
                CAST(COALESCE(s.cell_count, ss.total_cells) AS SIGNED) AS cells
            FROM oscar_sample s
            LEFT JOIN oscar_sample_stat ss ON ss.dataset_id = s.dataset_id
            WHERE s.dataset_id = #{datasetId}
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
            LIMIT 1
            """)
    SearchResultOverviewResponse selectOverviewByDatasetId(@Param("datasetId") String datasetId);

    /*
     * Cell type composition (donut chart): groups by domain-aware cluster column
     * and joins oscar_cluster_annotation for major_cell_type labelling.
     */
    @Select({
            "<script>",
            "SELECT",
            "    grouped.label AS label,",
            "    grouped.cellCount AS count,",
            "    grouped.cellCount / NULLIF(SUM(grouped.cellCount) OVER (), 0) AS ratio",
            "FROM (",
            "    SELECT",
            "        " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "        COUNT(*) AS cellCount",
            "    FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "    WHERE " + CELL_VISIBLE_FILTER,
            "      AND ${clusterExpr} IS NOT NULL",
            "    GROUP BY " + DOMAIN_CLUSTER_LABEL_EXPR,
            ") grouped",
            "ORDER BY grouped.cellCount DESC, grouped.label ASC",
            "</script>"
    })
    List<SearchResultCellTypeItemResponse> selectCellTypeComposition(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("clusterRaw") String clusterRaw,
            @Param("clusterExpr") String clusterExpr
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "</script>"
    })
    long countScatterPoints(
            @Param("datasetId") String datasetId,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn
    );

    /* Cluster scatter. Only integration may attach its WNN cell-type annotation. */
    @Select({
            "<script>",
            "SELECT",
            "    c.barcode,",
            "    ${xColumn} AS x,",
            "    ${yColumn} AS y,",
            "    " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "    CASE WHEN #{domain} = 'integration'",
            "         THEN COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown')",
            "         ELSE NULL END AS celltype,",
            "    COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown') AS cluster",
            "FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "<if test='samplingModulo != null and samplingModulo &gt; 1'>",
            "  AND MOD(CRC32(c.barcode), #{samplingModulo}) = 0",
            "</if>",
            "ORDER BY CRC32(c.barcode), c.barcode",
            "LIMIT #{maxPoints}",
            "</script>"
    })
    List<SearchResultUmapPointResponse> selectSampledScatterPoints(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn,
            @Param("clusterExpr") String clusterExpr,
            @Param("clusterRaw") String clusterRaw,
            @Param("samplingModulo") Integer samplingModulo,
            @Param("maxPoints") Integer maxPoints
    );

    /* Lightweight scatter (colorBy=cell_type, domain=integration only — no annotation JOIN) */
    @Select({
            "<script>",
            "SELECT",
            "    c.barcode,",
            "    ${xColumn} AS x,",
            "    ${yColumn} AS y,",
            "    COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown') AS label,",
            "    COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown') AS celltype,",
            "    COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown') AS cluster",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "<if test='samplingModulo != null and samplingModulo &gt; 1'>",
            "  AND MOD(CRC32(c.barcode), #{samplingModulo}) = 0",
            "</if>",
            "ORDER BY CRC32(c.barcode), c.barcode",
            "LIMIT #{maxPoints}",
            "</script>"
    })
    List<SearchResultUmapPointResponse> selectSampledScatterPointsByCellType(
            @Param("datasetId") String datasetId,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn,
            @Param("clusterExpr") String clusterExpr,
            @Param("samplingModulo") Integer samplingModulo,
            @Param("maxPoints") Integer maxPoints
    );

    @Select({
            "<script>",
            "SELECT",
            "    c.barcode,",
            "    ${xColumn} AS x,",
            "    ${yColumn} AS y,",
            "    " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "    CASE WHEN #{domain} = 'integration'",
            "         THEN COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown')",
            "         ELSE NULL END AS celltype,",
            "    COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown') AS cluster",
            "FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "<if test='afterBarcode != null'>",
            "  AND c.barcode &gt; #{afterBarcode}",
            "</if>",
            "ORDER BY c.barcode",
            "LIMIT #{pageSize}",
            "</script>"
    })
    List<SearchResultUmapPointResponse> selectScatterPointsPage(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn,
            @Param("clusterExpr") String clusterExpr,
            @Param("clusterRaw") String clusterRaw,
            @Param("afterBarcode") String afterBarcode,
            @Param("pageSize") Integer pageSize
    );

    @Select({
            "<script>",
            "SELECT",
            "    c.barcode,",
            "    ${xColumn} AS x,",
            "    ${yColumn} AS y,",
            "    COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown') AS label,",
            "    COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown') AS celltype,",
            "    COALESCE(NULLIF(TRIM(${clusterExpr}), ''), 'Unknown') AS cluster",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "<if test='afterBarcode != null'>",
            "  AND c.barcode &gt; #{afterBarcode}",
            "</if>",
            "ORDER BY c.barcode",
            "LIMIT #{pageSize}",
            "</script>"
    })
    List<SearchResultUmapPointResponse> selectScatterPointsPageByCellType(
            @Param("datasetId") String datasetId,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn,
            @Param("clusterExpr") String clusterExpr,
            @Param("afterBarcode") String afterBarcode,
            @Param("pageSize") Integer pageSize
    );

    @Select({
            "<script>",
            "SELECT",
            "    " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "    COUNT(*) AS count,",
            "    MIN(${metricColumn}) AS min,",
            "    MAX(${metricColumn}) AS max",
            "FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${metricColumn} IS NOT NULL",
            "  AND ${clusterExpr} IS NOT NULL",
            "GROUP BY " + DOMAIN_CLUSTER_LABEL_EXPR,
            "ORDER BY count DESC, label ASC",
            "</script>"
    })
    List<SearchResultQcSummaryRow> selectQcSummaryByMetric(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("clusterRaw") String clusterRaw,
            @Param("clusterExpr") String clusterExpr,
            @Param("metricColumn") String metricColumn
    );

    @Select({
            "<script>",
            "SELECT sampled.label, sampled.value",
            "FROM (",
            "    SELECT",
            "        " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "        ${metricColumn} AS value,",
            "        ROW_NUMBER() OVER (",
            "            PARTITION BY " + DOMAIN_CLUSTER_LABEL_EXPR,
            "            ORDER BY CRC32(c.barcode), c.barcode",
            "        ) AS rowNumber",
            "    FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "    WHERE " + CELL_VISIBLE_FILTER,
            "      AND ${metricColumn} IS NOT NULL",
            "      AND ${clusterExpr} IS NOT NULL",
            ") sampled",
            "WHERE sampled.rowNumber &lt;= #{maxValuesPerGroup}",
            "ORDER BY sampled.label ASC, sampled.rowNumber ASC",
            "</script>"
    })
    List<SearchResultQcValueRow> selectQcSampledValuesByMetric(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("clusterRaw") String clusterRaw,
            @Param("clusterExpr") String clusterExpr,
            @Param("metricColumn") String metricColumn,
            @Param("maxValuesPerGroup") Integer maxValuesPerGroup
    );

    @Select({
            "<script>",
            "SELECT",
            "    " + DOMAIN_CLUSTER_LABEL_EXPR + " AS label,",
            "    ${metricColumn} AS value",
            "FROM oscar_cell_profile c",
            CLUSTER_ANNOTATION_JOIN,
            "WHERE " + CELL_VISIBLE_FILTER,
            "  AND ${metricColumn} IS NOT NULL",
            "  AND ${clusterExpr} IS NOT NULL",
            "  AND MOD(CRC32(c.barcode), #{samplingModulo}) = 0",
            "</script>"
    })
    List<SearchResultQcValueRow> selectQcSampledValuesByModulo(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("clusterRaw") String clusterRaw,
            @Param("clusterExpr") String clusterExpr,
            @Param("metricColumn") String metricColumn,
            @Param("samplingModulo") Integer samplingModulo
    );

    
}
