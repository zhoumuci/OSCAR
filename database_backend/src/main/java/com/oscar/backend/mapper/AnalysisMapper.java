package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {

    String ENRICHMENT_SCOPE_FILTER = """
              AND gms.signal_type IN
              <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
            <if test='datasetId != null'>
              AND gms.dataset_id = #{datasetId}
            </if>
            """;

    String ENRICHMENT_LEVEL_SELECT = """
            <choose>
              <when test='resultLevel == "cluster"'>
                COALESCE(MAX(NULLIF(TRIM(gms.major_cell_type), '')), 'Unknown') AS cellType,
                COALESCE(NULLIF(TRIM(gms.group_name), ''), 'Unknown') AS context
              </when>
              <otherwise>
                COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') AS cellType,
                COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') AS context
              </otherwise>
            </choose>
            """;

    String ENRICHMENT_GROUP_BY = """
            <choose>
              <when test='resultLevel == "cluster"'>
                GROUP BY gms.dataset_id,
                         COALESCE(NULLIF(TRIM(gms.group_name), ''), 'Unknown')
              </when>
              <otherwise>
                GROUP BY COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')
              </otherwise>
            </choose>
            """;

    String ENRICHMENT_CANDIDATE_FILTER = """
            <if test='resultLevel != "cluster" and cellTypes != null and cellTypes.size > 0'>
              AND COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') IN
              <foreach item='cellType' collection='cellTypes' open='(' separator=',' close=')'>#{cellType}</foreach>
            </if>
            <if test='contexts != null and contexts.size > 0'>
              AND (
                <choose>
                  <when test='resultLevel == "cluster"'>
                    COALESCE(NULLIF(TRIM(gms.group_name), ''), 'Unknown')
                  </when>
                  <otherwise>
                    COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')
                  </otherwise>
                </choose>
              ) IN
              <foreach item='context' collection='contexts' open='(' separator=',' close=')'>#{context}</foreach>
            </if>
            """;

    @Select("""
            SELECT dataset_id, COALESCE(sample_name, dataset_name, dataset_id) AS sample_name
            FROM oscar_sample
            WHERE (is_deleted IS NULL OR is_deleted = 0)
              AND (is_visible IS NULL OR is_visible = 1)
            ORDER BY dataset_id
            """)
    List<Map<String, String>> selectAllDatasetIds();

    @Select("""
            <script>
            SELECT DISTINCT s.tissue
            FROM oscar_sample s FORCE INDEX (idx_sample_tissue)
            WHERE NULLIF(TRIM(s.tissue), '') IS NOT NULL
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_dataset)
                  WHERE gms.dataset_id = s.dataset_id
                    AND gms.domain = #{domain}
                    AND gms.signal_type IN
                    <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
              )
            ORDER BY s.tissue
            </script>
            """)
    List<String> selectCellTypeEnrichmentTissues(
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

    @Select("""
            <script>
            SELECT s.dataset_id,
                   COALESCE(NULLIF(TRIM(s.sample_name), ''), s.dataset_id) AS sample_name
            FROM oscar_sample s
            WHERE s.tissue = #{tissue}
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_dataset)
                  WHERE gms.dataset_id = s.dataset_id
                    AND gms.domain = #{domain}
                    AND gms.signal_type IN
                    <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
              )
            ORDER BY s.dataset_id
            </script>
            """)
    List<Map<String, Object>> selectCellTypeEnrichmentDatasets(
            @Param("tissue") String tissue,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM oscar_sample s
            WHERE s.tissue = #{tissue}
              AND s.dataset_id = #{datasetId}
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_dataset)
                  WHERE gms.dataset_id = s.dataset_id
                    AND gms.domain = #{domain}
                    AND gms.signal_type IN
                    <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
              )
            </script>
            """)
    int countCellTypeEnrichmentDatasetInTissue(
            @Param("tissue") String tissue,
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

    // ---- Sequence-to-Peak2Gene queries ----

    @Select("""
            <script>
            SELECT dataset_id, domain, peak_name, chromosome, peak_start, peak_end,
                   gene_name, correlation, fdr, link_score, source_file
            FROM oscar_peak_gene_link
            WHERE domain = 'integration'
              AND is_deleted = 0 AND is_visible = 1
              AND chromosome = #{chromosome}
              AND peak_start &lt; #{regionEnd}
              AND peak_end &gt; #{regionStart}
            <if test='datasetId != null'>
              AND dataset_id = #{datasetId}
            </if>
            ORDER BY link_score DESC
            <if test='limit &gt; 0'>
              LIMIT #{limit}
            </if>
            </script>
            """)
    List<Map<String, Object>> selectP2gLinksByRegion(
            @Param("chromosome") String chromosome,
            @Param("regionStart") long regionStart,
            @Param("regionEnd") long regionEnd,
            @Param("datasetId") String datasetId,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            WITH selected_marker_peaks AS (
              SELECT id AS marker_peak_id, dataset_id, domain, cluster_source, group_name, peak_name,
                     chromosome, peak_start, peak_end, log2fc, fdr, mean_diff, source_file
              FROM oscar_marker_peak
              WHERE domain = 'integration'
                AND chromosome = #{chromosome}
                AND peak_start &lt; #{regionEnd}
                AND peak_end &gt; #{regionStart}
              <if test='datasetId != null'>
                AND dataset_id = #{datasetId}
              </if>
              ORDER BY fdr ASC, log2fc DESC, id ASC
              <if test='limit &gt; 0'>
                LIMIT #{limit}
              </if>
            )
            SELECT mp.marker_peak_id,
                   mp.dataset_id, mp.domain, mp.cluster_source, mp.group_name, mp.peak_name,
                   mp.chromosome, mp.peak_start, mp.peak_end,
                   mp.log2fc, mp.fdr, mp.mean_diff, mp.source_file,
                   pgl.peak_name AS p2g_peak_name,
                   pgl.chromosome AS p2g_chromosome,
                   pgl.peak_start AS p2g_peak_start,
                   pgl.peak_end AS p2g_peak_end,
                   pgl.gene_name AS p2g_gene_name,
                   pgl.correlation AS p2g_correlation,
                   pgl.fdr AS p2g_fdr,
                   pgl.link_score AS p2g_link_score,
                   pgl.source_file AS p2g_source_file
            FROM selected_marker_peaks mp
            LEFT JOIN oscar_peak_gene_link pgl
              ON pgl.dataset_id = mp.dataset_id
             AND pgl.domain = mp.domain
             AND pgl.chromosome = mp.chromosome
             AND pgl.peak_start = mp.peak_start
             AND pgl.peak_end = mp.peak_end
             AND pgl.is_deleted = 0
             AND pgl.is_visible = 1
            ORDER BY mp.fdr ASC, mp.log2fc DESC, mp.marker_peak_id ASC,
                     pgl.link_score DESC, pgl.fdr ASC, pgl.gene_name ASC
            </script>
            """)
    List<Map<String, Object>> selectMarkerPeaksByRegion(
            @Param("chromosome") String chromosome,
            @Param("regionStart") long regionStart,
            @Param("regionEnd") long regionEnd,
            @Param("datasetId") String datasetId,
            @Param("limit") int limit
    );

    // ---- Cell type enrichment queries ----

    @Select("""
            <script>
            SELECT universe_size
            FROM oscar_gene_marker_enrichment_universe
            WHERE marker_reference = #{markerReference}
              AND scope_dataset_id = ''
            </script>
            """)
    Long selectGlobalEnrichmentUniverseSize(@Param("markerReference") String markerReference);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT gms.gene_symbol)
            FROM oscar_gene_marker_summary gms
            WHERE gms.domain = #{domain}
            """ + ENRICHMENT_SCOPE_FILTER + """
            </script>
            """)
    long countEnrichmentUniverseGenes(
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId
    );

    @Select("""
            <script>
            SELECT DISTINCT UPPER(gms.gene_symbol) AS geneSymbol
            FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_enrich_gene_scope)
            WHERE gms.domain = #{domain}
            """ + ENRICHMENT_SCOPE_FILTER + """
              AND gms.gene_symbol IN
              <foreach item='gene' collection='genes' open='(' separator=',' close=')'>#{gene}</foreach>
            ORDER BY geneSymbol
            </script>
            """)
    List<String> selectMatchedEnrichmentGenes(
            @Param("genes") List<String> genes,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM (
                SELECT 1
                FROM oscar_gene_marker_enrichment_set marker_set
                WHERE marker_set.marker_reference = #{markerReference}
                  AND marker_set.scope_dataset_id = ''
                  AND marker_set.result_level = #{resultLevel}
                <choose>
                  <when test='resultLevel == "cell_type"'>
                    GROUP BY COALESCE(NULLIF(TRIM(marker_set.cell_type), ''), 'Unknown')
                  </when>
                  <otherwise>
                    GROUP BY COALESCE(NULLIF(TRIM(marker_set.cell_type), ''), 'Unknown'),
                             marker_set.context_label
                  </otherwise>
                </choose>
            ) marker_sets
            </script>
            """)
    long countGlobalEnrichmentMarkerSets(
            @Param("markerReference") String markerReference,
            @Param("resultLevel") String resultLevel
    );

    @Select("""
            <script>
            SELECT cell_type AS cellType,
                   context_label AS context,
                   set_size AS setSize,
                   dataset_count AS datasetCount
            FROM oscar_gene_marker_enrichment_set
            WHERE marker_reference = #{markerReference}
              AND scope_dataset_id = ''
              AND result_level = #{resultLevel}
            <if test='cellTypes != null and cellTypes.size > 0'>
              AND cell_type IN
              <foreach item='cellType' collection='cellTypes' open='(' separator=',' close=')'>#{cellType}</foreach>
            </if>
            <if test='contexts != null and contexts.size > 0'>
              AND context_label IN
              <foreach item='context' collection='contexts' open='(' separator=',' close=')'>#{context}</foreach>
            </if>
            ORDER BY set_size DESC, cell_type ASC, context_label ASC
            </script>
            """)
    List<Map<String, Object>> selectGlobalCandidateEnrichmentMarkerSets(
            @Param("markerReference") String markerReference,
            @Param("resultLevel") String resultLevel,
            @Param("cellTypes") List<String> cellTypes,
            @Param("contexts") List<String> contexts
    );

    @Select("""
            <script>
            SELECT
            """ + ENRICHMENT_LEVEL_SELECT + """
              , COUNT(DISTINCT gms.gene_symbol) AS setSize
              , COUNT(DISTINCT gms.dataset_id) AS datasetCount
            FROM oscar_gene_marker_summary gms
            <if test='datasetId == null and resultLevel == "cell_type"'>
              FORCE INDEX (idx_gene_summary_enrich_celltype_gene_fast)
            </if>
            WHERE gms.domain = #{domain}
            """ + ENRICHMENT_SCOPE_FILTER + ENRICHMENT_GROUP_BY + """
            HAVING setSize > 0
            ORDER BY setSize DESC, cellType ASC, context ASC
            </script>
            """)
    List<Map<String, Object>> selectEnrichmentMarkerSets(
            @Param("resultLevel") String resultLevel,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId
    );

    /**
     * Count the complete hypothesis family for BH correction using the same
     * stored cell-type/context keys as the enrichment queries, rather
     * than only the marker sets that overlap the submitted genes.
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM (
                SELECT 1
                FROM oscar_gene_marker_summary gms
                WHERE gms.domain = #{domain}
                """ + ENRICHMENT_SCOPE_FILTER + """
                <choose>
                  <when test='resultLevel == "cluster"'>
                    GROUP BY gms.dataset_id,
                             COALESCE(NULLIF(TRIM(gms.group_name), ''), 'Unknown')
                  </when>
                  <otherwise>
                    GROUP BY COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')
                  </otherwise>
                </choose>
            ) marker_sets
            </script>
            """)
    long countEnrichmentMarkerSets(
            @Param("resultLevel") String resultLevel,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId
    );

    @Select("""
            <script>
            SELECT
            """ + ENRICHMENT_LEVEL_SELECT + """
              , COUNT(DISTINCT gms.gene_symbol) AS setSize
              , COUNT(DISTINCT gms.dataset_id) AS datasetCount
            FROM oscar_gene_marker_summary gms
            <if test='datasetId == null and resultLevel == "cell_type"'>
              FORCE INDEX (idx_gene_summary_enrich_celltype_gene_fast)
            </if>
            WHERE gms.domain = #{domain}
            """ + ENRICHMENT_SCOPE_FILTER + ENRICHMENT_CANDIDATE_FILTER + ENRICHMENT_GROUP_BY + """
            HAVING setSize > 0
            ORDER BY setSize DESC, cellType ASC, context ASC
            </script>
            """)
    List<Map<String, Object>> selectCandidateEnrichmentMarkerSets(
            @Param("resultLevel") String resultLevel,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId,
            @Param("cellTypes") List<String> cellTypes,
            @Param("contexts") List<String> contexts
    );

    /**
     * Compute set sizes only for the exact dataset/cell-type pairs touched by
     * the input genes.  Keeping the pair relation in SQL avoids the very large
     * independent cell-type and dataset IN lists (and their Cartesian
     * superset) used by the generic candidate query.
     */
    @Select("""
            <script>
            SELECT
                COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') AS cellType,
                gms.dataset_id AS context,
                COUNT(DISTINCT gms.gene_symbol) AS setSize,
                1 AS datasetCount
            FROM (
                SELECT DISTINCT
                    candidate.dataset_id,
                    COALESCE(NULLIF(TRIM(candidate.major_cell_type), ''), 'Unknown') AS cell_type
                FROM oscar_gene_marker_summary candidate
                    FORCE INDEX (idx_gene_summary_enrich_gene_scope)
                WHERE candidate.domain = #{domain}
                  AND candidate.signal_type IN
                  <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
                  AND candidate.gene_symbol IN
                  <foreach item='gene' collection='genes' open='(' separator=',' close=')'>#{gene}</foreach>
            ) candidate_sets
            STRAIGHT_JOIN oscar_gene_marker_summary gms
                FORCE INDEX (idx_gene_summary_enrich_dataset_cluster)
              ON gms.domain = #{domain}
             AND gms.signal_type IN
              <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
             AND gms.dataset_id = candidate_sets.dataset_id
            WHERE COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') = candidate_sets.cell_type
            GROUP BY
                gms.dataset_id,
                COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')
            ORDER BY setSize DESC, cellType ASC, context ASC
            </script>
            """)
    List<Map<String, Object>> selectDatasetCellTypeCandidateEnrichmentMarkerSets(
            @Param("genes") List<String> genes,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

    @Select("""
            <script>
            SELECT
            """ + ENRICHMENT_LEVEL_SELECT + """
              , UPPER(gms.gene_symbol) AS geneSymbol
            FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_enrich_gene_scope)
            WHERE gms.domain = #{domain}
            """ + ENRICHMENT_SCOPE_FILTER + """
              AND gms.gene_symbol IN
              <foreach item='gene' collection='genes' open='(' separator=',' close=')'>#{gene}</foreach>
            """ + ENRICHMENT_GROUP_BY + """
              , UPPER(gms.gene_symbol)
            ORDER BY cellType ASC, context ASC, geneSymbol ASC
            </script>
            """)
    List<Map<String, Object>> selectEnrichmentOverlapGenes(
            @Param("genes") List<String> genes,
            @Param("resultLevel") String resultLevel,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes,
            @Param("datasetId") String datasetId
    );

    /** Sample lookup by cell type. */
    @Select("SELECT s.dataset_id, s.sample_name, s.tissue, s.sample_type, s.cell_count, s.disease "
            + "FROM oscar_sample s "
            + "JOIN oscar_cluster_annotation ca ON ca.dataset_id = s.dataset_id "
            + "WHERE NULLIF(TRIM(ca.major_cell_type), '') = #{cellType} "
            + "  AND (s.is_deleted IS NULL OR s.is_deleted = 0) "
            + "  AND (s.is_visible IS NULL OR s.is_visible = 1) "
            + "GROUP BY s.dataset_id "
            + "ORDER BY s.cell_count DESC")
    List<Map<String, Object>> selectSamplesByCellType(@Param("cellType") String cellType);

    /** Sample lookup by tissue. */
    @Select("SELECT dataset_id, sample_name, tissue, sample_type, cell_count, disease "
            + "FROM oscar_sample "
            + "WHERE (is_deleted IS NULL OR is_deleted = 0) "
            + "  AND (is_visible IS NULL OR is_visible = 1) "
            + "  AND tissue = #{tissue} "
            + "ORDER BY cell_count DESC")
    List<Map<String, Object>> selectSamplesByTissue(@Param("tissue") String tissue);
}
