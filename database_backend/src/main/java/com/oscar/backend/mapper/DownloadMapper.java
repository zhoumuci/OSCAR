package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DownloadMapper {

    @Select("""
        SELECT
            s.dataset_id,
            s.sample_name,
            s.sample_type,
            s.tissue,
            s.disease,
            s.sample_source,
            s.cell_count,
            s.platform,
            s.source_id
        FROM oscar_sample s
        WHERE (s.is_deleted IS NULL OR s.is_deleted = 0)
          AND (s.is_visible IS NULL OR s.is_visible = 1)
        ORDER BY s.sort_order ASC, s.dataset_id ASC
        """)
    List<Map<String, Object>> listDownloadSamples();

    @Select("""
        <script>
        SELECT
            mg.gene_symbol,
            NULLIF(mg.group_name, '') AS group_name,
            mg.avg_log2fc,
            mg.fdr,
            mg.mean_diff,
            mg.signal_type
        FROM oscar_marker_gene mg
        WHERE mg.dataset_id = #{datasetId}
          AND mg.domain = #{domain}
          AND (mg.is_deleted IS NULL OR mg.is_deleted = 0)
          <choose>
            <when test="signalType == 'gene_expression'">
              AND mg.signal_type IN ('gene_expression', 'gene_exp')
            </when>
            <when test="signalType == 'gene_score'">
              AND mg.signal_type = 'gene_score'
            </when>
          </choose>
        ORDER BY mg.gene_symbol, mg.group_name
        </script>
                """)
    List<Map<String, Object>> selectMarkerGenes(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("signalType") String signalType);

    @Select("""
        SELECT
            mp.peak_name,
            mp.chromosome,
            mp.peak_start,
            mp.peak_end,
            NULLIF(mp.group_name, '') AS group_name,
            mp.log2fc,
            mp.fdr,
            mp.mean_diff
        FROM oscar_marker_peak mp
        WHERE mp.dataset_id = #{datasetId}
          AND mp.domain = #{domain}
        ORDER BY mp.chromosome, mp.peak_start
                """)
    List<Map<String, Object>> selectMarkerPeaks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain);

    @Select("""
        SELECT
            p.gene_name   AS gene_symbol,
            p.peak_name,
            p.chromosome  AS peak_chromosome,
            p.peak_start,
            p.peak_end,
            p.link_score,
            p.correlation,
            p.fdr         AS link_fdr,
            p.var_q_rna   AS var_q_rna,
            p.var_q_atac  AS var_q_atac,
            p.cell_type,
            NULLIF(p.cell_type, '') AS standard_cell_type
        FROM oscar_peak_gene_link p
        WHERE p.dataset_id = #{datasetId}
          AND p.domain = #{domain}
          AND p.is_visible = 1
          AND p.is_deleted = 0
                """)
    List<Map<String, Object>> selectP2gLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain);

    @Select("""
        SELECT
            mlr.gene_symbol,
            mlr.peak_name,
            mlr.peak_chromosome,
            mlr.peak_start,
            mlr.peak_end,
            mlr.link_score,
            mlr.correlation,
            mlr.link_fdr,
            NULLIF(TRIM(mlr.cell_type), '') AS cell_type,
            NULLIF(TRIM(mlr.group_name), '') AS cluster,
            mlr.gene_log2fc,
            mlr.gene_fdr,
            mlr.gene_mean_diff,
            mlr.peak_log2fc,
            mlr.peak_fdr,
            mlr.peak_mean_diff,
            NULLIF(TRIM(mlr.signal_type), '') AS signal_type
        FROM oscar_marker_linked_region mlr
        WHERE mlr.dataset_id = #{datasetId}
          AND mlr.domain = #{domain}
          AND mlr.has_marker_peak = 1
        ORDER BY mlr.link_score DESC, mlr.gene_symbol ASC
                """)
    List<Map<String, Object>> selectP2gMarkerLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain);
}
