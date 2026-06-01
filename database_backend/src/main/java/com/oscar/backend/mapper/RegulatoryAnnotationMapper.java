package com.oscar.backend.mapper;

import com.oscar.backend.entity.RegulatoryAnnotationContextOption;
import com.oscar.backend.entity.RegulatoryAnnotationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegulatoryAnnotationMapper {

    String MARKER_GENE_SELECT = """
            SELECT
                'marker_gene' AS recordKind,
                mg.id AS markerGeneId,
                CAST(NULL AS SIGNED) AS markerPeakId,
                CAST(NULL AS SIGNED) AS peakGeneLinkId,
                mg.dataset_id AS datasetId,
                mg.domain AS domain,
                NULLIF(TRIM(mg.group_name), '') AS groupName,
                NULLIF(TRIM(mg.gene_symbol), '') AS geneSymbol,
                NULLIF(TRIM(mg.gene_id), '') AS geneId,
                NULLIF(TRIM(mg.chromosome), '') AS geneChromosome,
                mg.gene_start AS geneStart,
                mg.gene_end AS geneEnd,
                NULLIF(TRIM(mg.strand), '') AS strand,
                mg.avg_log2fc AS avgLog2fc,
                mg.fdr AS geneFdr,
                mg.mean_diff AS geneMeanDiff,
                NULLIF(TRIM(mg.source_file), '') AS markerGeneSourceFile,
                CAST(NULL AS CHAR) AS peakName,
                CAST(NULL AS CHAR) AS peakChromosome,
                CAST(NULL AS SIGNED) AS peakStart,
                CAST(NULL AS SIGNED) AS peakEnd,
                NULL AS peakLog2fc,
                NULL AS peakFdr,
                NULL AS peakMeanDiff,
                CAST(NULL AS CHAR) AS markerPeakSourceFile,
                CAST(NULL AS CHAR) AS linkedGeneName,
                NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                NULLIF(TRIM(mg.group_name), '') AS clusterLabel,
                CAST(NULL AS CHAR) AS tfName,
                NULL AS linkScore,
                NULL AS correlation,
                NULL AS linkFdr,
                CAST(NULL AS CHAR) AS linkSource
            FROM oscar_marker_gene mg
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mg.dataset_id
             AND ca.domain = mg.domain
             AND ca.cluster_label = mg.group_name
            """;

    String MARKER_PEAK_SELECT = """
            SELECT
                'marker_peak' AS recordKind,
                CAST(NULL AS SIGNED) AS markerGeneId,
                mp.id AS markerPeakId,
                CAST(NULL AS SIGNED) AS peakGeneLinkId,
                mp.dataset_id AS datasetId,
                mp.domain AS domain,
                NULLIF(TRIM(mp.group_name), '') AS groupName,
                CAST(NULL AS CHAR) AS geneSymbol,
                CAST(NULL AS CHAR) AS geneId,
                CAST(NULL AS CHAR) AS geneChromosome,
                CAST(NULL AS SIGNED) AS geneStart,
                CAST(NULL AS SIGNED) AS geneEnd,
                CAST(NULL AS CHAR) AS strand,
                NULL AS avgLog2fc,
                NULL AS geneFdr,
                NULL AS geneMeanDiff,
                CAST(NULL AS CHAR) AS markerGeneSourceFile,
                NULLIF(TRIM(mp.peak_name), '') AS peakName,
                NULLIF(TRIM(mp.chromosome), '') AS peakChromosome,
                mp.peak_start AS peakStart,
                mp.peak_end AS peakEnd,
                mp.log2fc AS peakLog2fc,
                mp.fdr AS peakFdr,
                mp.mean_diff AS peakMeanDiff,
                NULLIF(TRIM(mp.source_file), '') AS markerPeakSourceFile,
                CAST(NULL AS CHAR) AS linkedGeneName,
                NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                NULLIF(TRIM(mp.group_name), '') AS clusterLabel,
                CAST(NULL AS CHAR) AS tfName,
                NULL AS linkScore,
                CAST(NULL AS SIGNED) AS linkedGeneCount,
                NULL AS correlation,
                NULL AS linkFdr,
                CAST(NULL AS CHAR) AS linkSource
            FROM oscar_marker_peak mp
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mp.dataset_id
             AND ca.domain = mp.domain
             AND ca.cluster_label = mp.group_name
            """;

    String LINKED_REGION_SELECT = """
            SELECT
                'linked_region' AS recordKind,
                mlr.id AS linkedRegionId,
                mlr.marker_gene_id AS markerGeneId,
                mlr.marker_peak_id AS markerPeakId,
                mlr.peak_gene_link_id AS peakGeneLinkId,
                mlr.dataset_id AS datasetId,
                mlr.domain AS domain,
                NULLIF(TRIM(mlr.group_name), '') AS groupName,
                NULLIF(TRIM(mlr.gene_symbol), '') AS geneSymbol,
                NULLIF(TRIM(mlr.gene_id), '') AS geneId,
                NULLIF(TRIM(mlr.gene_chromosome), '') AS geneChromosome,
                mlr.gene_start AS geneStart,
                mlr.gene_end AS geneEnd,
                NULLIF(TRIM(mlr.strand), '') AS strand,
                mlr.gene_log2fc AS avgLog2fc,
                mlr.gene_fdr AS geneFdr,
                mlr.gene_mean_diff AS geneMeanDiff,
                CAST(NULL AS CHAR) AS markerGeneSourceFile,
                NULLIF(TRIM(mlr.peak_name), '') AS peakName,
                NULLIF(TRIM(mlr.peak_region), '') AS peakRegion,
                NULLIF(TRIM(mlr.peak_chromosome), '') AS peakChromosome,
                mlr.peak_start AS peakStart,
                mlr.peak_end AS peakEnd,
                mlr.peak_log2fc AS peakLog2fc,
                mlr.peak_fdr AS peakFdr,
                mlr.peak_mean_diff AS peakMeanDiff,
                CAST(NULL AS CHAR) AS markerPeakSourceFile,
                NULLIF(TRIM(mlr.gene_symbol), '') AS linkedGeneName,
                NULLIF(TRIM(mlr.cell_type), '') AS cellType,
                COALESCE(NULLIF(TRIM(mlr.cluster_label), ''), NULLIF(TRIM(mlr.group_name), '')) AS clusterLabel,
                NULLIF(TRIM(mlr.context_label), '') AS contextLabel,
                CAST(NULL AS CHAR) AS tfName,
                mlr.link_score AS linkScore,
                CAST(NULL AS SIGNED) AS linkedGeneCount,
                mlr.correlation AS correlation,
                mlr.link_fdr AS linkFdr,
                NULLIF(TRIM(mlr.source), '') AS linkSource
            FROM oscar_marker_linked_region mlr
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
                context_rows.cellType AS cellType,
                context_rows.clusterLabel AS cluster,
                COUNT(*) AS count
            FROM (
                SELECT
                    NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                    NULLIF(TRIM(mg.group_name), '') AS clusterLabel
                FROM oscar_marker_gene mg
                LEFT JOIN oscar_cluster_annotation ca
                  ON ca.dataset_id = mg.dataset_id
                 AND ca.domain = mg.domain
                 AND ca.cluster_label = mg.group_name
                WHERE mg.dataset_id = #{datasetId}
                  AND mg.domain = #{domain}
            ) context_rows
            WHERE context_rows.cellType IS NOT NULL
               OR context_rows.clusterLabel IS NOT NULL
            GROUP BY context_rows.cellType, context_rows.clusterLabel
            ORDER BY context_rows.cellType IS NULL,
                     context_rows.cellType ASC,
                     context_rows.clusterLabel IS NULL,
                     context_rows.clusterLabel ASC
            """)
    List<RegulatoryAnnotationContextOption> selectMarkerGeneContextOptions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                context_rows.cellType AS cellType,
                context_rows.clusterLabel AS cluster,
                COUNT(*) AS count
            FROM (
                SELECT
                    NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                    NULLIF(TRIM(mp.group_name), '') AS clusterLabel
                FROM oscar_marker_peak mp
                LEFT JOIN oscar_cluster_annotation ca
                  ON ca.dataset_id = mp.dataset_id
                 AND ca.domain = mp.domain
                 AND ca.cluster_label = mp.group_name
                WHERE mp.dataset_id = #{datasetId}
                  AND mp.domain = #{domain}
            ) context_rows
            WHERE context_rows.cellType IS NOT NULL
               OR context_rows.clusterLabel IS NOT NULL
            GROUP BY context_rows.cellType, context_rows.clusterLabel
            ORDER BY context_rows.cellType IS NULL,
                     context_rows.cellType ASC,
                     context_rows.clusterLabel IS NULL,
                     context_rows.clusterLabel ASC
            """)
    List<RegulatoryAnnotationContextOption> selectMarkerPeakContextOptions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                context_rows.cellType AS cellType,
                context_rows.clusterLabel AS cluster,
                COUNT(*) AS count
            FROM (
                SELECT
                    NULLIF(TRIM(mlr.cell_type), '') AS cellType,
                    COALESCE(NULLIF(TRIM(mlr.cluster_label), ''), NULLIF(TRIM(mlr.group_name), '')) AS clusterLabel
                FROM oscar_marker_linked_region mlr
                WHERE mlr.dataset_id = #{datasetId}
                  AND mlr.domain = #{domain}
            ) context_rows
            WHERE context_rows.cellType IS NOT NULL
               OR context_rows.clusterLabel IS NOT NULL
            GROUP BY context_rows.cellType, context_rows.clusterLabel
            ORDER BY context_rows.cellType IS NULL,
                     context_rows.cellType ASC,
                     context_rows.clusterLabel IS NULL,
                     context_rows.clusterLabel ASC
            """)
    List<RegulatoryAnnotationContextOption> selectLinkedRegionContextOptions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM oscar_marker_gene mg",
            "LEFT JOIN oscar_cluster_annotation ca",
            "  ON ca.dataset_id = mg.dataset_id",
            " AND ca.domain = mg.domain",
            " AND ca.cluster_label = mg.group_name",
            "WHERE mg.dataset_id = #{datasetId}",
            "  AND mg.domain = #{domain}",
            "<if test='targetGene != null'>",
            "  AND UPPER(TRIM(mg.gene_symbol)) = #{targetGene}",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND mg.group_name = #{contextCluster}",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND ca.major_cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND mg.fdr IS NOT NULL AND mg.fdr &lt;= #{maxFdr}",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND mg.avg_log2fc IS NOT NULL AND ABS(mg.avg_log2fc) &gt;= #{minLog2fc}",
            "</if>",
            "</script>"
    })
    long countMarkerGenes(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("targetGene") String targetGene,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc
    );

    @Select({
            "<script>",
            MARKER_GENE_SELECT,
            "WHERE mg.dataset_id = #{datasetId}",
            "  AND mg.domain = #{domain}",
            "<if test='targetGene != null'>",
            "  AND UPPER(TRIM(mg.gene_symbol)) = #{targetGene}",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND mg.group_name = #{contextCluster}",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND ca.major_cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND mg.fdr IS NOT NULL AND mg.fdr &lt;= #{maxFdr}",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND mg.avg_log2fc IS NOT NULL AND ABS(mg.avg_log2fc) &gt;= #{minLog2fc}",
            "</if>",
            "ORDER BY mg.group_name ASC, mg.avg_log2fc DESC, mg.gene_symbol ASC, mg.id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectMarkerGenes(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("targetGene") String targetGene,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM oscar_marker_peak mp",
            "<if test='contextCellType != null'>",
            "LEFT JOIN oscar_cluster_annotation ca",
            "  ON ca.dataset_id = mp.dataset_id",
            " AND ca.domain = mp.domain",
            " AND ca.cluster_label = mp.group_name",
            "</if>",
            "WHERE mp.dataset_id = #{datasetId}",
            "  AND mp.domain = #{domain}",
            "<if test='targetGene != null'>",
            "  AND EXISTS (",
            "      SELECT 1",
            "      FROM oscar_peak_gene_link pgl_filter",
            "      WHERE pgl_filter.dataset_id = mp.dataset_id",
            "        AND pgl_filter.domain = mp.domain",
            "        AND pgl_filter.chromosome = mp.chromosome",
            "        AND pgl_filter.peak_start = mp.peak_start",
            "        AND pgl_filter.peak_end = mp.peak_end",
            "        AND UPPER(TRIM(pgl_filter.gene_name)) = #{targetGene}",
            "        AND (pgl_filter.is_deleted IS NULL OR pgl_filter.is_deleted = 0)",
            "        AND (pgl_filter.is_visible IS NULL OR pgl_filter.is_visible = 1)",
            "  )",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mp.peak_name LIKE #{peakPattern}",
            "      OR CONCAT(COALESCE(mp.chromosome, ''), ':', COALESCE(CAST(mp.peak_start AS CHAR), ''), '-', COALESCE(CAST(mp.peak_end AS CHAR), '')) LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND mp.group_name = #{contextCluster}",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND ca.major_cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND mp.fdr IS NOT NULL AND mp.fdr &lt;= #{maxFdr}",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND mp.log2fc IS NOT NULL AND ABS(mp.log2fc) &gt;= #{minLog2fc}",
            "</if>",
            "</script>"
    })
    long countMarkerPeaks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc
    );

    @Select({
            "<script>",
            "SELECT mp.id",
            "FROM oscar_marker_peak mp",
            "<if test='contextCellType != null'>",
            "LEFT JOIN oscar_cluster_annotation ca",
            "  ON ca.dataset_id = mp.dataset_id",
            " AND ca.domain = mp.domain",
            " AND ca.cluster_label = mp.group_name",
            "</if>",
            "WHERE mp.dataset_id = #{datasetId}",
            "  AND mp.domain = #{domain}",
            "<if test='targetGene != null'>",
            "  AND EXISTS (",
            "      SELECT 1",
            "      FROM oscar_peak_gene_link pgl_filter",
            "      WHERE pgl_filter.dataset_id = mp.dataset_id",
            "        AND pgl_filter.domain = mp.domain",
            "        AND pgl_filter.chromosome = mp.chromosome",
            "        AND pgl_filter.peak_start = mp.peak_start",
            "        AND pgl_filter.peak_end = mp.peak_end",
            "        AND UPPER(TRIM(pgl_filter.gene_name)) = #{targetGene}",
            "        AND (pgl_filter.is_deleted IS NULL OR pgl_filter.is_deleted = 0)",
            "        AND (pgl_filter.is_visible IS NULL OR pgl_filter.is_visible = 1)",
            "  )",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mp.peak_name LIKE #{peakPattern}",
            "      OR CONCAT(COALESCE(mp.chromosome, ''), ':', COALESCE(CAST(mp.peak_start AS CHAR), ''), '-', COALESCE(CAST(mp.peak_end AS CHAR), '')) LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND mp.group_name = #{contextCluster}",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND ca.major_cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND mp.fdr IS NOT NULL AND mp.fdr &lt;= #{maxFdr}",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND mp.log2fc IS NOT NULL AND ABS(mp.log2fc) &gt;= #{minLog2fc}",
            "</if>",
            "ORDER BY mp.group_name ASC, mp.log2fc DESC, mp.chromosome ASC, mp.peak_start ASC, mp.id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Long> selectMarkerPeakPageIds(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            MARKER_PEAK_SELECT,
            "WHERE mp.id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "ORDER BY FIELD(mp.id,",
            "<foreach collection='ids' item='id' separator=','>",
            "  #{id}",
            "</foreach>",
            ")",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectMarkerPeaksByIds(@Param("ids") List<Long> ids);

    @Select({
            "<script>",
            MARKER_PEAK_SELECT,
            "WHERE mp.dataset_id = #{datasetId}",
            "  AND mp.domain = #{domain}",
            "<if test='targetGene != null'>",
            "  AND EXISTS (",
            "      SELECT 1",
            "      FROM oscar_peak_gene_link pgl_filter",
            "      WHERE pgl_filter.dataset_id = mp.dataset_id",
            "        AND pgl_filter.domain = mp.domain",
            "        AND pgl_filter.chromosome = mp.chromosome",
            "        AND pgl_filter.peak_start = mp.peak_start",
            "        AND pgl_filter.peak_end = mp.peak_end",
            "        AND UPPER(TRIM(pgl_filter.gene_name)) = #{targetGene}",
            "        AND (pgl_filter.is_deleted IS NULL OR pgl_filter.is_deleted = 0)",
            "        AND (pgl_filter.is_visible IS NULL OR pgl_filter.is_visible = 1)",
            "  )",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mp.peak_name LIKE #{peakPattern}",
            "      OR CONCAT(COALESCE(mp.chromosome, ''), ':', COALESCE(CAST(mp.peak_start AS CHAR), ''), '-', COALESCE(CAST(mp.peak_end AS CHAR), '')) LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND mp.group_name = #{contextCluster}",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND ca.major_cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND mp.fdr IS NOT NULL AND mp.fdr &lt;= #{maxFdr}",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND mp.log2fc IS NOT NULL AND ABS(mp.log2fc) &gt;= #{minLog2fc}",
            "</if>",
            "ORDER BY mp.group_name ASC, mp.log2fc DESC, mp.chromosome ASC, mp.peak_start ASC, mp.id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectMarkerPeaks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            "WITH requested_peaks AS (",
            "  <foreach collection='peaks' item='peak' separator=' UNION ALL '>",
            "  SELECT",
            "      #{peak.markerPeakId} AS markerPeakId,",
            "      #{peak.peakChromosome} AS peakChromosome,",
            "      #{peak.peakStart} AS peakStart,",
            "      #{peak.peakEnd} AS peakEnd",
            "  </foreach>",
            "), link_counts AS (",
            "  SELECT",
            "      rp.markerPeakId AS markerPeakId,",
            "      COUNT(DISTINCT pgl.gene_name) AS linkedGeneCount",
            "  FROM requested_peaks rp",
            "  JOIN oscar_peak_gene_link pgl",
            "    ON pgl.dataset_id = #{datasetId}",
            "   AND pgl.domain = #{domain}",
            "   AND pgl.chromosome = rp.peakChromosome",
            "   AND pgl.peak_start = rp.peakStart",
            "   AND pgl.peak_end = rp.peakEnd",
            "   AND (pgl.is_deleted IS NULL OR pgl.is_deleted = 0)",
            "   AND (pgl.is_visible IS NULL OR pgl.is_visible = 1)",
            "  WHERE pgl.gene_name IS NOT NULL",
            "    AND TRIM(pgl.gene_name) &lt;&gt; ''",
            "  GROUP BY rp.markerPeakId",
            "), ranked_links AS (",
            "  SELECT",
            "      rp.markerPeakId AS markerPeakId,",
            "      NULLIF(TRIM(pgl.gene_name), '') AS linkedGeneName,",
            "      lc.linkedGeneCount AS linkedGeneCount,",
            "      COALESCE(pgl.link_score, ABS(pgl.correlation)) AS linkScore,",
            "      pgl.correlation AS correlation,",
            "      pgl.fdr AS linkFdr,",
            "      NULLIF(TRIM(pgl.source), '') AS linkSource,",
            "      ROW_NUMBER() OVER (",
            "          PARTITION BY rp.markerPeakId",
            "          ORDER BY COALESCE(pgl.link_score, ABS(pgl.correlation), 0) DESC,",
            "                   COALESCE(pgl.fdr, 1) ASC,",
            "                   pgl.gene_name ASC,",
            "                   pgl.id ASC",
            "      ) AS rowRank",
            "  FROM requested_peaks rp",
            "  JOIN oscar_peak_gene_link pgl",
            "    ON pgl.dataset_id = #{datasetId}",
            "   AND pgl.domain = #{domain}",
            "   AND pgl.chromosome = rp.peakChromosome",
            "   AND pgl.peak_start = rp.peakStart",
            "   AND pgl.peak_end = rp.peakEnd",
            "   AND (pgl.is_deleted IS NULL OR pgl.is_deleted = 0)",
            "   AND (pgl.is_visible IS NULL OR pgl.is_visible = 1)",
            "  JOIN link_counts lc",
            "    ON lc.markerPeakId = rp.markerPeakId",
            "  WHERE pgl.gene_name IS NOT NULL",
            "    AND TRIM(pgl.gene_name) &lt;&gt; ''",
            ")",
            "SELECT",
            "    'marker_peak_link_summary' AS recordKind,",
            "    CAST(NULL AS SIGNED) AS markerGeneId,",
            "    markerPeakId AS markerPeakId,",
            "    CAST(NULL AS SIGNED) AS peakGeneLinkId,",
            "    #{datasetId} AS datasetId,",
            "    #{domain} AS domain,",
            "    CAST(NULL AS CHAR) AS groupName,",
            "    CAST(NULL AS CHAR) AS geneSymbol,",
            "    CAST(NULL AS CHAR) AS geneId,",
            "    CAST(NULL AS CHAR) AS geneChromosome,",
            "    CAST(NULL AS SIGNED) AS geneStart,",
            "    CAST(NULL AS SIGNED) AS geneEnd,",
            "    CAST(NULL AS CHAR) AS strand,",
            "    NULL AS avgLog2fc,",
            "    NULL AS geneFdr,",
            "    NULL AS geneMeanDiff,",
            "    CAST(NULL AS CHAR) AS markerGeneSourceFile,",
            "    CAST(NULL AS CHAR) AS peakName,",
            "    CAST(NULL AS CHAR) AS peakChromosome,",
            "    CAST(NULL AS SIGNED) AS peakStart,",
            "    CAST(NULL AS SIGNED) AS peakEnd,",
            "    NULL AS peakLog2fc,",
            "    NULL AS peakFdr,",
            "    NULL AS peakMeanDiff,",
            "    CAST(NULL AS CHAR) AS markerPeakSourceFile,",
            "    linkedGeneName AS linkedGeneName,",
            "    CAST(NULL AS CHAR) AS cellType,",
            "    CAST(NULL AS CHAR) AS clusterLabel,",
            "    CAST(NULL AS CHAR) AS tfName,",
            "    linkScore AS linkScore,",
            "    linkedGeneCount AS linkedGeneCount,",
            "    correlation AS correlation,",
            "    linkFdr AS linkFdr,",
            "    linkSource AS linkSource",
            "FROM ranked_links",
            "WHERE rowRank = 1",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectMarkerPeakLinkSummaries(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peaks") List<RegulatoryAnnotationRow> peaks
    );

    @Select("""
            SELECT COUNT(*)
            FROM oscar_marker_linked_region mlr
            WHERE mlr.dataset_id = #{datasetId}
              AND mlr.domain = #{dataDomain}
            """)
    long countLinkedRegionMaterializedRows(
            @Param("datasetId") String datasetId,
            @Param("dataDomain") String dataDomain
    );

    @Select("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM oscar_marker_linked_region mlr
                WHERE mlr.dataset_id = #{datasetId}
                  AND mlr.domain = #{dataDomain}
                LIMIT 1
            ) THEN 1 ELSE 0 END
            """)
    int existsLinkedRegionMaterializedRows(
            @Param("datasetId") String datasetId,
            @Param("dataDomain") String dataDomain
    );

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT mlr.peak_gene_link_id)",
            "FROM oscar_marker_linked_region mlr",
            "WHERE mlr.dataset_id = #{datasetId}",
            "  AND mlr.domain = #{dataDomain}",
            "  AND mlr.peak_gene_link_id IS NOT NULL",
            "  AND mlr.gene_symbol IS NOT NULL",
            "  AND TRIM(mlr.gene_symbol) &lt;&gt; ''",
            "<if test='targetGene != null'>",
            "  AND UPPER(TRIM(mlr.gene_symbol)) = #{targetGene}",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mlr.peak_name LIKE #{peakPattern}",
            "      OR mlr.peak_region LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND (mlr.cluster_label = #{contextCluster} OR (mlr.cluster_label IS NULL AND mlr.group_name = #{contextCluster}))",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND mlr.cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND (mlr.gene_fdr &lt;= #{maxFdr} OR mlr.peak_fdr &lt;= #{maxFdr})",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND (ABS(mlr.gene_log2fc) &gt;= #{minLog2fc} OR ABS(mlr.peak_log2fc) &gt;= #{minLog2fc})",
            "</if>",
            "</script>"
    })
    long countLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("dataDomain") String dataDomain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc
    );

    @Select({
            "<script>",
            "SELECT MIN(mlr.id)",
            "FROM oscar_marker_linked_region mlr",
            "WHERE mlr.dataset_id = #{datasetId}",
            "  AND mlr.domain = #{dataDomain}",
            "  AND mlr.peak_gene_link_id IS NOT NULL",
            "  AND mlr.gene_symbol IS NOT NULL",
            "  AND TRIM(mlr.gene_symbol) &lt;&gt; ''",
            "<if test='targetGene != null'>",
            "  AND UPPER(TRIM(mlr.gene_symbol)) = #{targetGene}",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mlr.peak_name LIKE #{peakPattern}",
            "      OR mlr.peak_region LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND (mlr.cluster_label = #{contextCluster} OR (mlr.cluster_label IS NULL AND mlr.group_name = #{contextCluster}))",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND mlr.cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND (mlr.gene_fdr &lt;= #{maxFdr} OR mlr.peak_fdr &lt;= #{maxFdr})",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND (ABS(mlr.gene_log2fc) &gt;= #{minLog2fc} OR ABS(mlr.peak_log2fc) &gt;= #{minLog2fc})",
            "</if>",
            "GROUP BY mlr.peak_gene_link_id",
            "ORDER BY",
            "  MAX(COALESCE(mlr.link_score, ABS(mlr.correlation), 0)) DESC,",
            "  MIN(COALESCE(mlr.link_fdr, 1)) ASC,",
            "  MIN(mlr.peak_start) ASC,",
            "  MIN(mlr.id) ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Long> selectLinkedRegionPageIds(
            @Param("datasetId") String datasetId,
            @Param("dataDomain") String dataDomain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            LINKED_REGION_SELECT,
            "WHERE mlr.id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "ORDER BY FIELD(mlr.id,",
            "<foreach collection='ids' item='id' separator=','>",
            "  #{id}",
            "</foreach>",
            ")",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectLinkedRegionsByIds(@Param("ids") List<Long> ids);

    @Select({
            "<script>",
            LINKED_REGION_SELECT,
            "WHERE mlr.dataset_id = #{datasetId}",
            "  AND mlr.domain = #{dataDomain}",
            "<if test='targetGene != null'>",
            "  AND UPPER(TRIM(mlr.gene_symbol)) = #{targetGene}",
            "</if>",
            "<if test='peakPattern != null'>",
            "  AND (",
            "      mlr.peak_name LIKE #{peakPattern}",
            "      OR mlr.peak_region LIKE #{peakPattern}",
            "  )",
            "</if>",
            "<if test='contextCluster != null'>",
            "  AND (mlr.cluster_label = #{contextCluster} OR (mlr.cluster_label IS NULL AND mlr.group_name = #{contextCluster}))",
            "</if>",
            "<if test='contextCellType != null'>",
            "  AND mlr.cell_type = #{contextCellType}",
            "</if>",
            "<if test='maxFdr != null'>",
            "  AND (mlr.gene_fdr &lt;= #{maxFdr} OR mlr.peak_fdr &lt;= #{maxFdr})",
            "</if>",
            "<if test='minLog2fc != null'>",
            "  AND (ABS(mlr.gene_log2fc) &gt;= #{minLog2fc} OR ABS(mlr.peak_log2fc) &gt;= #{minLog2fc})",
            "</if>",
            "ORDER BY mlr.group_name ASC, mlr.gene_symbol ASC, mlr.link_score DESC, mlr.peak_start ASC, mlr.id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RegulatoryAnnotationRow> selectLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("dataDomain") String dataDomain,
            @Param("targetGene") String targetGene,
            @Param("peakPattern") String peakPattern,
            @Param("contextCellType") String contextCellType,
            @Param("contextCluster") String contextCluster,
            @Param("maxFdr") Double maxFdr,
            @Param("minLog2fc") Double minLog2fc,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}
