package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PeakGeneContextMapper {

    String RAW_CANDIDATE_FILTER = ""
            + " FROM oscar_sample s "
            + " STRAIGHT_JOIN oscar_peak_gene_link pgl FORCE INDEX (idx_pgl_dataset_domain_gene_deleted) "
            + "   ON pgl.dataset_id = s.dataset_id "
            + "  AND pgl.domain = 'integration' "
            + "  AND pgl.is_deleted = 0 "
            + "  AND pgl.is_visible = 1 "
            + " WHERE s.tissue = #{tissue} "
            + "   AND (s.is_deleted IS NULL OR s.is_deleted = 0) "
            + "   AND (s.is_visible IS NULL OR s.is_visible = 1) "
            + "   <if test='datasetId != null'>AND s.dataset_id = #{datasetId}</if> "
            + "   AND pgl.gene_name IN "
            + "   <foreach item='g' collection='genes' open='(' separator=',' close=')'>#{g}</foreach> ";

    String MARKER_CANDIDATE_FILTER = ""
            + " FROM oscar_sample s "
            + " STRAIGHT_JOIN oscar_peak_gene_link pgl FORCE INDEX (idx_pgl_dataset_domain_gene_deleted) "
            + "   ON pgl.dataset_id = s.dataset_id "
            + "  AND pgl.domain = 'integration' "
            + "  AND pgl.is_deleted = 0 "
            + "  AND pgl.is_visible = 1 "
            + " STRAIGHT_JOIN oscar_marker_linked_region mlr FORCE INDEX (idx_mlr_dataset_domain_link_dedup) "
            + "   ON mlr.dataset_id = pgl.dataset_id "
            + "  AND mlr.domain = pgl.domain "
            + "  AND mlr.peak_gene_link_id = pgl.id "
            + "  AND mlr.has_marker_peak = 1 "
            + " WHERE s.tissue = #{tissue} "
            + "   AND (s.is_deleted IS NULL OR s.is_deleted = 0) "
            + "   AND (s.is_visible IS NULL OR s.is_visible = 1) "
            + "   AND NULLIF(TRIM(mlr.context_label), '') IS NOT NULL "
            + "   AND NULLIF(TRIM(mlr.signal_type), '') IS NOT NULL "
            + "   <if test='requireCellType'>AND NULLIF(TRIM(mlr.cell_type), '') IS NOT NULL</if> "
            + "   <if test='datasetId != null'>AND s.dataset_id = #{datasetId}</if> "
            + "   AND pgl.gene_name IN "
            + "   <foreach item='g' collection='genes' open='(' separator=',' close=')'>#{g}</foreach> ";

    @Select("""
            SELECT DISTINCT s.tissue
            FROM oscar_sample s FORCE INDEX (idx_sample_tissue)
            WHERE NULLIF(TRIM(s.tissue), '') IS NOT NULL
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_peak_gene_link pgl FORCE INDEX (idx_pgl_dataset_visible)
                  WHERE pgl.dataset_id = s.dataset_id
                    AND pgl.domain = 'integration'
                    AND pgl.is_deleted = 0
                    AND pgl.is_visible = 1
              )
            ORDER BY s.tissue
            """)
    List<String> selectP2gTissues();

    @Select("""
            SELECT s.dataset_id, COALESCE(NULLIF(TRIM(s.sample_name), ''), s.dataset_id) AS sample_name
            FROM oscar_sample s
            WHERE s.tissue = #{tissue}
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_peak_gene_link pgl FORCE INDEX (idx_pgl_dataset_visible)
                  WHERE pgl.dataset_id = s.dataset_id
                    AND pgl.domain = 'integration'
                    AND pgl.is_deleted = 0
                    AND pgl.is_visible = 1
              )
            ORDER BY s.dataset_id
            """)
    List<Map<String, Object>> selectP2gDatasets(@Param("tissue") String tissue);

    @Select("""
            SELECT COUNT(*)
            FROM oscar_sample s
            WHERE s.tissue = #{tissue}
              AND s.dataset_id = #{datasetId}
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
              AND EXISTS (
                  SELECT 1
                  FROM oscar_peak_gene_link pgl FORCE INDEX (idx_pgl_dataset_visible)
                  WHERE pgl.dataset_id = s.dataset_id
                    AND pgl.domain = 'integration'
                    AND pgl.is_deleted = 0
                    AND pgl.is_visible = 1
              )
            """)
    int countP2gDatasetInTissue(@Param("tissue") String tissue, @Param("datasetId") String datasetId);

    @Select("<script>SELECT pgl.id AS evidence_id, pgl.dataset_id, pgl.domain, "
            + "pgl.peak_name, pgl.chromosome, pgl.peak_start, pgl.peak_end, "
            + "pgl.gene_name, COALESCE(pgl.link_score, ABS(pgl.correlation), 0) AS link_score, "
            + "pgl.correlation, pgl.fdr AS link_fdr "
            + RAW_CANDIDATE_FILTER
            + "</script>")
    List<Map<String, Object>> selectRawCandidates(
            @Param("genes") List<String> genes,
            @Param("tissue") String tissue,
            @Param("datasetId") String datasetId
    );

    @Select("<script>SELECT mlr.id AS evidence_id, mlr.peak_gene_link_id, mlr.dataset_id, mlr.domain, "
            + "mlr.peak_name, mlr.peak_chromosome AS chromosome, mlr.peak_start, mlr.peak_end, "
            + "mlr.gene_symbol AS gene_name, "
            + "NULLIF(TRIM(mlr.cell_type), '') AS cell_type, "
            + "mlr.context_label, mlr.cluster_label, LOWER(TRIM(mlr.signal_type)) AS signal_type, "
            + "mlr.has_marker_peak, COALESCE(mlr.link_score, ABS(mlr.correlation), 0) AS link_score, "
            + "mlr.correlation, mlr.link_fdr "
            + MARKER_CANDIDATE_FILTER
            + "</script>")
    List<Map<String, Object>> selectMarkerCandidates(
            @Param("genes") List<String> genes,
            @Param("tissue") String tissue,
            @Param("datasetId") String datasetId,
            @Param("requireCellType") boolean requireCellType
    );

    @Select("<script>SELECT DISTINCT mg.dataset_id, mg.gene_symbol, NULLIF(TRIM(mg.signal_type), '') AS signal_type "
            + "FROM oscar_marker_gene mg FORCE INDEX (idx_marker_dataset_domain_gene) "
            + "WHERE mg.domain = 'integration' AND mg.is_deleted = 0 "
            + "AND mg.dataset_id IN <foreach item='id' collection='datasetIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "AND mg.gene_symbol IN <foreach item='g' collection='genes' open='(' separator=',' close=')'>#{g}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectGeneMarkerSignals(
            @Param("datasetIds") List<String> datasetIds,
            @Param("genes") List<String> genes
    );

    @Select("<script>SELECT DISTINCT mp.dataset_id, mp.peak_name "
            + "FROM oscar_marker_peak mp FORCE INDEX (idx_marker_peak_dataset_domain_peak) "
            + "WHERE mp.domain = 'integration' "
            + "AND mp.dataset_id IN <foreach item='id' collection='datasetIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "AND mp.peak_name IN <foreach item='peak' collection='peakNames' open='(' separator=',' close=')'>#{peak}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectMarkerPeakKeys(
            @Param("datasetIds") List<String> datasetIds,
            @Param("peakNames") List<String> peakNames
    );
}
