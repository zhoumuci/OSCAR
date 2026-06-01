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

    @Select("""
            SELECT COUNT(*)
            FROM oscar_sample
            WHERE dataset_id = #{datasetId}
              AND (is_deleted IS NULL OR is_deleted = 0)
              AND (is_visible IS NULL OR is_visible = 1)
            """)
    long countVisibleSampleByDatasetId(@Param("datasetId") String datasetId);

    @Select("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'oscar_cell_profile'
              AND column_name IN ('domain', 'view_type', 'modality')
            ORDER BY FIELD(column_name, 'domain', 'view_type', 'modality')
            LIMIT 1
            """)
    String selectCellProfileDomainColumn();

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
     * SearchResult is sample-detail level. dataset_id is the business key and
     * oscar_cell_profile is used only for detail charts, not Browse table logic.
     * Dynamic column values are internal whitelist outputs.
     */
    @Select({
            "<script>",
            "SELECT",
            "    grouped.label AS label,",
            "    grouped.cellCount AS count,",
            "    grouped.cellCount / NULLIF(SUM(grouped.cellCount) OVER (), 0) AS ratio",
            "FROM (",
            "    SELECT",
            "        COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown') AS label,",
            "        COUNT(*) AS cellCount",
            "    FROM oscar_cell_profile c",
            "    WHERE " + CELL_VISIBLE_FILTER,
            "<if test='domainColumn != null'>",
            "      AND LOWER(TRIM(${domainColumn})) = #{domain}",
            "</if>",
            "    GROUP BY COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown')",
            ") grouped",
            "ORDER BY grouped.cellCount DESC, grouped.label ASC",
            "</script>"
    })
    List<SearchResultCellTypeItemResponse> selectCellTypeComposition(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("domainColumn") String domainColumn,
            @Param("groupColumn") String groupColumn
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "<if test='domainColumn != null'>",
            "  AND LOWER(TRIM(${domainColumn})) = #{domain}",
            "</if>",
            "  AND ${xColumn} IS NOT NULL",
            "  AND ${yColumn} IS NOT NULL",
            "</script>"
    })
    long countScatterPoints(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("domainColumn") String domainColumn,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn
    );

    @Select({
            "<script>",
            "SELECT",
            "    c.barcode,",
            "    ${xColumn} AS x,",
            "    ${yColumn} AS y,",
            "    COALESCE(NULLIF(TRIM(${colorColumn}), ''), 'Unknown') AS label,",
            "    COALESCE(NULLIF(TRIM(c.cell_type), ''), 'Unknown') AS celltype,",
            "    COALESCE(NULLIF(TRIM(c.cluster_label), ''), 'Unknown') AS cluster",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "<if test='domainColumn != null'>",
            "  AND LOWER(TRIM(${domainColumn})) = #{domain}",
            "</if>",
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
            @Param("domainColumn") String domainColumn,
            @Param("xColumn") String xColumn,
            @Param("yColumn") String yColumn,
            @Param("colorColumn") String colorColumn,
            @Param("samplingModulo") Integer samplingModulo,
            @Param("maxPoints") Integer maxPoints
    );

    @Select({
            "<script>",
            "SELECT",
            "    COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown') AS label,",
            "    COUNT(*) AS count,",
            "    MIN(${metricColumn}) AS min,",
            "    MAX(${metricColumn}) AS max",
            "FROM oscar_cell_profile c",
            "WHERE " + CELL_VISIBLE_FILTER,
            "<if test='domainColumn != null'>",
            "  AND LOWER(TRIM(${domainColumn})) = #{domain}",
            "</if>",
            "  AND ${metricColumn} IS NOT NULL",
            "GROUP BY COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown')",
            "ORDER BY count DESC, label ASC",
            "</script>"
    })
    List<SearchResultQcSummaryRow> selectQcSummaryByMetric(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("domainColumn") String domainColumn,
            @Param("groupColumn") String groupColumn,
            @Param("metricColumn") String metricColumn
    );

    @Select({
            "<script>",
            "SELECT sampled.label, sampled.value",
            "FROM (",
            "    SELECT",
            "        COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown') AS label,",
            "        ${metricColumn} AS value,",
            "        ROW_NUMBER() OVER (",
            "            PARTITION BY COALESCE(NULLIF(TRIM(${groupColumn}), ''), 'Unknown')",
            "            ORDER BY CRC32(c.barcode), c.barcode",
            "        ) AS rowNumber",
            "    FROM oscar_cell_profile c",
            "    WHERE " + CELL_VISIBLE_FILTER,
            "<if test='domainColumn != null'>",
            "      AND LOWER(TRIM(${domainColumn})) = #{domain}",
            "</if>",
            "      AND ${metricColumn} IS NOT NULL",
            ") sampled",
            "WHERE sampled.rowNumber &lt;= #{maxValuesPerGroup}",
            "ORDER BY sampled.label ASC, sampled.rowNumber ASC",
            "</script>"
    })
    List<SearchResultQcValueRow> selectQcSampledValuesByMetric(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("domainColumn") String domainColumn,
            @Param("groupColumn") String groupColumn,
            @Param("metricColumn") String metricColumn,
            @Param("maxValuesPerGroup") Integer maxValuesPerGroup
    );
}
