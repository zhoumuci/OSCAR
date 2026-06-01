package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegulatoryAnnotationMaintenanceMapper {

    @Delete("""
            DELETE FROM oscar_marker_linked_region
            WHERE dataset_id = #{datasetId}
              AND domain = #{domain}
            """)
    int deleteMarkerLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Insert("""
            INSERT INTO oscar_marker_linked_region (
                dataset_id,
                domain,
                group_name,
                cluster_label,
                cell_type,
                context_label,

                marker_gene_id,
                gene_symbol,
                gene_id,
                gene_chromosome,
                gene_start,
                gene_end,
                strand,
                gene_log2fc,
                gene_fdr,
                gene_mean_diff,

                peak_gene_link_id,
                peak_name,
                peak_chromosome,
                peak_start,
                peak_end,
                peak_region,

                marker_peak_id,
                peak_log2fc,
                peak_fdr,
                peak_mean_diff,

                link_score,
                correlation,
                link_fdr,

                has_marker_peak,
                source
            )
            SELECT
                mg.dataset_id,
                mg.domain,
                mg.group_name,
                mg.group_name AS cluster_label,
                ca.major_cell_type AS cell_type,
                CASE
                    WHEN ca.major_cell_type IS NOT NULL AND ca.major_cell_type <> ''
                    THEN CONCAT(ca.major_cell_type, ' / ', mg.group_name)
                    ELSE mg.group_name
                END AS context_label,

                mg.id AS marker_gene_id,
                mg.gene_symbol,
                mg.gene_id,
                mg.chromosome AS gene_chromosome,
                mg.gene_start,
                mg.gene_end,
                mg.strand,
                mg.avg_log2fc AS gene_log2fc,
                mg.fdr AS gene_fdr,
                mg.mean_diff AS gene_mean_diff,

                pgl.id AS peak_gene_link_id,
                pgl.peak_name,
                pgl.chromosome AS peak_chromosome,
                pgl.peak_start,
                pgl.peak_end,
                CONCAT(pgl.chromosome, ':', pgl.peak_start, '-', pgl.peak_end) AS peak_region,

                mp.id AS marker_peak_id,
                mp.log2fc AS peak_log2fc,
                mp.fdr AS peak_fdr,
                mp.mean_diff AS peak_mean_diff,

                pgl.link_score,
                pgl.correlation,
                pgl.fdr AS link_fdr,

                CASE WHEN mp.id IS NOT NULL THEN 1 ELSE 0 END AS has_marker_peak,
                CASE
                    WHEN mp.id IS NOT NULL THEN 'materialized_marker_gene_peak_link'
                    ELSE 'materialized_marker_gene_link'
                END AS source
            FROM oscar_marker_gene mg
            JOIN oscar_peak_gene_link pgl
              ON pgl.dataset_id = mg.dataset_id
             AND pgl.domain = mg.domain
             AND pgl.gene_name = mg.gene_symbol
             AND pgl.is_deleted = 0
             AND pgl.is_visible = 1
            LEFT JOIN oscar_marker_peak mp
              ON mp.dataset_id = mg.dataset_id
             AND mp.domain = mg.domain
             AND mp.group_name = mg.group_name
             AND mp.chromosome = pgl.chromosome
             AND mp.peak_start = pgl.peak_start
             AND mp.peak_end = pgl.peak_end
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mg.dataset_id
             AND ca.domain = mg.domain
             AND ca.cluster_label = mg.group_name
            WHERE mg.dataset_id = #{datasetId}
              AND mg.domain = #{domain}
            """)
    int insertMarkerLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );
}
