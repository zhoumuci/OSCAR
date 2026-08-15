package com.oscar.backend.mapper;

import com.oscar.backend.entity.PeakSearchRequest;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {

    /**
     * Discover matching samples from the complete marker summary.  The
     * gene/domain-leading index turns every search into one contiguous range
     * scan instead of one random marker-index probe per sample.  record_count
     * preserves the raw marker-record semantics used by the search summary and
     * marker-record summary.
     */
    @Select("<script>"
            + "SELECT s.dataset_id, s.sample_name, s.tissue, s.sample_type, s.cell_count, "
            + "       s.platform, s.source_id, s.disease, s.sample_source, "
            + "       gene_stats.gene_count, gene_stats.total_evidence "
            + "FROM ("
            + "  SELECT per_gene.dataset_id, "
            + "         COUNT(*) AS gene_count, "
            + "         SUM(per_gene.gene_evidence) AS total_evidence "
            + "  FROM ("
            + "    SELECT gms.gene_symbol, gms.dataset_id, "
            + "           SUM(gms.record_count) AS gene_evidence "
            + "    FROM oscar_gene_marker_summary gms"
            + "    WHERE gms.gene_symbol IN "
            + "      <foreach item='g' collection='genes' open='(' separator=',' close=')'>#{g}</foreach> "
            + "    <if test='domain != null'> AND gms.domain = #{domain}</if> "
            + "    <if test='signalType != null'> AND gms.signal_type = #{signalType}</if> "
            + "    GROUP BY gms.gene_symbol, gms.dataset_id"
            + "  ) per_gene "
            + "  GROUP BY per_gene.dataset_id "
            + "  <if test='intersection'>"
            + "  HAVING COUNT(*) = #{geneCount}"
            + "  </if>"
            + ") gene_stats "
            + "STRAIGHT_JOIN oscar_sample s "
            + "  ON s.dataset_id = gene_stats.dataset_id "
            + "WHERE 1 = 1 "
            + "  AND (s.is_deleted IS NULL OR s.is_deleted = 0) "
            + "  AND (s.is_visible IS NULL OR s.is_visible = 1) "
            + "<if test='tissue != null'> AND s.tissue = #{tissue}</if> "
            + "</script>")
    List<Map<String, Object>> findGeneSampleStats(
            @Param("genes") List<String> genes,
            @Param("geneCount") int geneCount,
            @Param("intersection") boolean intersection,
            @Param("domain") String domain,
            @Param("signalType") String signalType,
            @Param("tissue") String tissue
    );

    /** Enrich with sample metadata. */
    @Select("<script>"
            + "SELECT s.dataset_id, s.sample_name, s.tissue, s.sample_type, s.cell_count, "
            + "       s.platform, s.source_id, s.disease, s.sample_source "
            + "FROM oscar_sample s "
            + "WHERE s.dataset_id IN "
            + "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> "
            + "  AND (s.is_deleted IS NULL OR s.is_deleted = 0) "
            + "  AND (s.is_visible IS NULL OR s.is_visible = 1) "
            + "<if test='tissue != null'> AND s.tissue = #{tissue}</if>"
            + "ORDER BY s.sample_name ASC"
            + "</script>")
    List<Map<String, Object>> getSampleMeta(@Param("ids") List<String> ids, @Param("tissue") String tissue);

    /** Count linked peaks and the strongest link per final-scope dataset. */
    @Select("<script>"
            + "SELECT mlr.dataset_id, COUNT(DISTINCT mlr.peak_name) AS linked_peaks, "
            + "       MAX(mlr.link_score) AS max_link_score "
            + "FROM oscar_marker_linked_region mlr FORCE INDEX (idx_mlr_gene_rep_ds) "
            + "WHERE mlr.gene_symbol IN "
            + "<foreach item='g' collection='genes' open='(' separator=',' close=')'>#{g}</foreach> "
            + "  AND mlr.is_representative = 1 "
            + "  AND mlr.dataset_id IN "
            + "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> "
            + "<if test='domain != null'> AND mlr.domain = #{domain}</if> "
            + "GROUP BY mlr.dataset_id"
            + "</script>")
    List<Map<String, Object>> getLinkedPeakStats(
            @Param("ids") List<String> ids,
            @Param("genes") List<String> genes,
            @Param("domain") String domain
    );

    /** Match and aggregate marker peaks across all input regions. */
    @Select({
            "<script>",
            "WITH query_regions AS (",
            "  <foreach collection='regions' item='region' index='idx' separator=' UNION ALL '>",
            "    SELECT #{idx} AS region_idx, #{region.chrom} AS chromosome,",
            "           #{region.start} AS region_start, #{region.end} AS region_end",
            "  </foreach>",
            ")",
            "SELECT s.dataset_id, s.sample_name, s.tissue, s.sample_type, s.cell_count,",
            "       s.platform, s.source_id, s.disease, s.sample_source,",
            "       peak_stats.matched_regions, peak_stats.peak_count, peak_stats.matched_region_ids",
            "FROM (",
            "  SELECT mp.dataset_id,",
            "         COUNT(DISTINCT qr.region_idx) AS matched_regions,",
            "         COUNT(DISTINCT mp.chromosome, mp.peak_start, mp.peak_end) AS peak_count,",
            "         GROUP_CONCAT(DISTINCT qr.region_idx ORDER BY qr.region_idx SEPARATOR ',') AS matched_region_ids",
            "  FROM query_regions qr",
            "  STRAIGHT_JOIN oscar_marker_peak mp FORCE INDEX (idx_marker_peak_dataset_domain_region)",
            "    ON mp.dataset_id = #{datasetId}",
            "   AND mp.domain = #{domain}",
            "   AND mp.chromosome = qr.chromosome",
            "   AND mp.peak_start &lt;= qr.region_end",
            "   AND mp.peak_end &gt;= qr.region_start",
            "  GROUP BY mp.dataset_id",
            "  <if test='matchAll'>HAVING COUNT(DISTINCT qr.region_idx) = #{regionCount}</if>",
            ") peak_stats",
            "STRAIGHT_JOIN oscar_sample s ON s.dataset_id = peak_stats.dataset_id",
            "WHERE (s.is_deleted IS NULL OR s.is_deleted = 0)",
            "  AND (s.is_visible IS NULL OR s.is_visible = 1)",
            "ORDER BY peak_stats.peak_count DESC, s.sample_name ASC, s.dataset_id ASC",
            "</script>"
    })
    List<Map<String, Object>> findSamplesByPeaks(
            @Param("regions") List<PeakSearchRequest.Region> regions,
            @Param("regionCount") int regionCount,
            @Param("matchAll") boolean matchAll,
            @Param("domain") String domain,
            @Param("datasetId") String datasetId
    );

    /** Fast path for the common single-region marker-peak search. */
    @Select({
            "<script>",
            "SELECT s.dataset_id, s.sample_name, s.tissue, s.sample_type, s.cell_count,",
            "       s.platform, s.source_id, s.disease, s.sample_source,",
            "       1 AS matched_regions, peak_stats.peak_count,",
            "       '0' AS matched_region_ids",
            "FROM (",
            "  SELECT mp.dataset_id, COUNT(DISTINCT mp.peak_start, mp.peak_end) AS peak_count",
            "  FROM oscar_marker_peak mp FORCE INDEX (idx_marker_peak_dataset_domain_region)",
            "  WHERE mp.dataset_id = #{datasetId}",
            "    AND mp.domain = #{domain}",
            "    AND mp.chromosome = #{region.chrom}",
            "    AND mp.peak_start &lt;= #{region.end}",
            "    AND mp.peak_end &gt;= #{region.start}",
            "  GROUP BY mp.dataset_id",
            ") peak_stats",
            "STRAIGHT_JOIN oscar_sample s ON s.dataset_id = peak_stats.dataset_id",
            "WHERE (s.is_deleted IS NULL OR s.is_deleted = 0)",
            "  AND (s.is_visible IS NULL OR s.is_visible = 1)",
            "ORDER BY peak_stats.peak_count DESC, s.sample_name ASC, s.dataset_id ASC",
            "</script>"
    })
    List<Map<String, Object>> findSamplesBySinglePeakRegion(
            @Param("region") PeakSearchRequest.Region region,
            @Param("domain") String domain,
            @Param("datasetId") String datasetId
    );

    /** Enrich final marker-peak samples with distinct linked genes without filtering them out. */
    @Select({
            "<script>",
            "WITH query_regions AS (",
            "  <foreach collection='regions' item='region' separator=' UNION ALL '>",
            "    SELECT #{region.chrom} AS chromosome,",
            "           #{region.start} AS region_start, #{region.end} AS region_end",
            "  </foreach>",
            ")",
            "SELECT mlr.dataset_id, COUNT(DISTINCT mlr.gene_symbol) AS gene_count",
            "FROM query_regions qr",
            "STRAIGHT_JOIN oscar_marker_linked_region mlr FORCE INDEX (idx_mlr_dataset_domain_region)",
            "  ON mlr.dataset_id = #{datasetId}",
            " AND mlr.domain = #{domain}",
            " AND mlr.peak_chromosome = qr.chromosome",
            " AND mlr.peak_start &lt;= qr.region_end",
            " AND mlr.peak_end &gt;= qr.region_start",
            "WHERE mlr.is_representative = 1",
            "  AND mlr.gene_symbol IS NOT NULL",
            "  AND TRIM(mlr.gene_symbol) &lt;&gt; ''",
            "GROUP BY mlr.dataset_id",
            "</script>"
    })
    List<Map<String, Object>> findLinkedGeneCountsForPeaks(
            @Param("regions") List<PeakSearchRequest.Region> regions,
            @Param("domain") String domain,
            @Param("datasetId") String datasetId
    );

    /** Fast path for linked-gene enrichment of one region. */
    @Select("""
            SELECT mlr.dataset_id, COUNT(DISTINCT mlr.gene_symbol) AS gene_count
            FROM oscar_marker_linked_region mlr FORCE INDEX (idx_mlr_dataset_domain_region)
            WHERE mlr.dataset_id = #{datasetId}
              AND mlr.domain = #{domain}
              AND mlr.peak_chromosome = #{region.chrom}
              AND mlr.peak_start <= #{region.end}
              AND mlr.peak_end >= #{region.start}
              AND mlr.is_representative = 1
              AND mlr.gene_symbol IS NOT NULL
              AND TRIM(mlr.gene_symbol) <> ''
            GROUP BY mlr.dataset_id
            """)
    List<Map<String, Object>> findLinkedGeneCountsForSinglePeakRegion(
            @Param("region") PeakSearchRequest.Region region,
            @Param("domain") String domain,
            @Param("datasetId") String datasetId
    );

    /* ── Tissue search ── */
    @Select("<script>"
            + "SELECT dataset_id AS datasetId, sample_name AS sampleName, tissue, "
            + "       sample_type AS cellContext, cell_count AS cellCount, "
            + "       platform, source_id AS sourceId, disease, sample_source AS sampleSource "
            + "FROM oscar_sample "
            + "WHERE tissue IN "
            + "  <foreach item='t' collection='tissues' open='(' separator=',' close=')'>#{t}</foreach> "
            + "  AND (is_deleted IS NULL OR is_deleted = 0) "
            + "  AND (is_visible IS NULL OR is_visible = 1) "
            + "ORDER BY cell_count DESC "
            + "</script>")
    List<Map<String, Object>> findSamplesByTissue(@Param("tissues") List<String> tissues);

    /* ── Cell type search ── */
    @Select("""
            SELECT ca.dataset_id, COUNT(DISTINCT ca.cluster_label) AS cluster_count,
                   SUM(COALESCE(ca.cell_count, 0)) AS matched_cell_count,
                   s.sample_name, s.tissue, s.sample_type, s.cell_count,
                   s.platform, s.source_id, s.disease, s.sample_source
            FROM oscar_cluster_annotation ca
            JOIN oscar_sample s ON s.dataset_id = ca.dataset_id
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
            WHERE ca.domain = 'integration'
              AND ca.major_cell_type = #{cellType}
            GROUP BY ca.dataset_id
            ORDER BY matched_cell_count DESC, ca.dataset_id ASC
            """)
    List<Map<String, Object>> findSamplesByCellType(@Param("cellType") String cellType);

    @Select("""
            SELECT DISTINCT NULLIF(ca.major_cell_type, '') AS major_cell_type
            FROM oscar_cluster_annotation ca
            WHERE ca.major_cell_type IS NOT NULL
              AND TRIM(ca.major_cell_type) <> ''
              AND ca.domain = 'integration'
            ORDER BY major_cell_type
            """)
    List<String> listDistinctCellTypes();

    @Select("SELECT tissue, COUNT(*) AS cnt FROM oscar_sample WHERE tissue IS NOT NULL AND TRIM(tissue) <> '' AND (is_deleted IS NULL OR is_deleted = 0) AND (is_visible IS NULL OR is_visible = 1) GROUP BY tissue ORDER BY cnt DESC")
    List<Map<String, Object>> tissueCounts();

    @Select("""
            SELECT NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                   COUNT(DISTINCT ca.dataset_id) AS cnt
            FROM oscar_cluster_annotation ca
            STRAIGHT_JOIN oscar_sample s ON s.dataset_id = ca.dataset_id
            WHERE ca.major_cell_type IS NOT NULL
              AND TRIM(ca.major_cell_type) <> ''
              AND ca.domain = 'integration'
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
            GROUP BY NULLIF(TRIM(ca.major_cell_type), '')
            ORDER BY cnt DESC, cellType ASC
            """)
    List<Map<String, Object>> cellTypeCounts();
}
