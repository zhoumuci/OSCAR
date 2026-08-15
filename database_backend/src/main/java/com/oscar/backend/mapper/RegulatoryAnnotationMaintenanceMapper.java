package com.oscar.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegulatoryAnnotationMaintenanceMapper {

    // =========================================================================
    // linked_region refresh
    // =========================================================================

    @Delete("""
            DELETE FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    int deleteMarkerLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Insert("""
            INSERT INTO oscar_marker_linked_region (
                dataset_id,
                domain,
                cluster_source,
                signal_type,
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
                is_representative,
                source,
                source_file
            )
            SELECT
                mg.dataset_id,
                mg.domain,
                COALESCE(mg.cluster_source, '') AS cluster_source,
                COALESCE(mg.signal_type, '') AS signal_type,
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
                CASE WHEN ROW_NUMBER() OVER (
                    PARTITION BY pgl.id
                    ORDER BY
                        COALESCE(pgl.link_score, ABS(pgl.correlation), 0) DESC,
                        COALESCE(pgl.fdr, 1) ASC,
                        pgl.peak_start ASC,
                        pgl.id ASC
                ) = 1 THEN 1 ELSE 0 END AS is_representative,
                'materialized_linked_region' AS source,
                CONCAT_WS(';',
                    NULLIF(mg.source_file, ''),
                    NULLIF(mp.source_file, ''),
                    NULLIF(pgl.source_file, '')
                ) AS source_file
            FROM oscar_marker_gene mg
            JOIN oscar_peak_gene_link pgl
              ON pgl.dataset_id = mg.dataset_id
             AND pgl.domain = mg.domain
             AND pgl.gene_name = mg.gene_symbol
             AND pgl.is_deleted = 0
            LEFT JOIN oscar_marker_peak mp
              ON mp.dataset_id = pgl.dataset_id
             AND mp.domain = mg.domain
             AND mp.peak_name = pgl.peak_name
             AND mp.group_name = mg.group_name
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mg.dataset_id
             AND ca.domain = mg.domain
             AND ca.cluster_label = mg.group_name
            WHERE mg.domain = #{domain}
              AND pgl.domain = #{domain}
              AND mg.gene_symbol IS NOT NULL
              AND mg.gene_symbol <> ''
              AND pgl.peak_name IS NOT NULL
              AND pgl.peak_name <> ''
              AND mg.is_deleted = 0
              AND (mg.dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    int insertMarkerLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT DISTINCT dataset_id
            FROM oscar_marker_gene
            WHERE domain = #{domain}
              AND is_deleted = 0
              AND gene_symbol IS NOT NULL AND gene_symbol <> ''
            ORDER BY dataset_id
            """)
    List<String> getDistinctDatasetIdsForDomain(@Param("domain") String domain);

    @Select("""
            SELECT DISTINCT dataset_id
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND is_representative = 1
            ORDER BY dataset_id
            """)
    List<String> getCompletedLinkedRegionDatasetIds(@Param("domain") String domain);

    // =========================================================================
    // linked_region post-refresh statistics
    // =========================================================================

    @Select("""
            SELECT COUNT(*)
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countLinkedRegions(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT COUNT(DISTINCT gene_symbol)
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countDistinctGenesInLinkedRegion(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT COUNT(DISTINCT peak_name)
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countDistinctPeaksInLinkedRegion(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT COUNT(*)
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND marker_peak_id IS NOT NULL
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countLinkedRegionWithMarkerPeak(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                COALESCE(signal_type, '') AS signal_type,
                COUNT(*) AS cnt
            FROM oscar_marker_linked_region
            WHERE domain = #{domain}
              AND (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            GROUP BY signal_type
            """)
    List<Map<String, Object>> getSignalTypeCounts(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    // =========================================================================
    // gene_marker_summary refresh
    // =========================================================================

    @Delete("""
            DELETE FROM oscar_gene_marker_summary
            WHERE (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    int deleteGeneMarkerSummary(@Param("datasetId") String datasetId);

    @Insert("""
            INSERT INTO oscar_gene_marker_summary (
                gene_symbol,
                domain,
                cluster_source,
                signal_type,
                dataset_id,
                group_name,
                major_cell_type,
                display_context,
                record_count,
                min_fdr,
                max_log2fc,
                max_mean_diff
            )
            SELECT
                mg.gene_symbol,
                mg.domain,
                COALESCE(mg.cluster_source, '') AS cluster_source,
                COALESCE(mg.signal_type, '') AS signal_type,
                mg.dataset_id,
                mg.group_name,
                COALESCE(ca.major_cell_type, mg.group_name, 'Unknown') AS major_cell_type,
                CASE
                    WHEN ca.major_cell_type IS NOT NULL AND ca.major_cell_type <> ''
                    THEN CONCAT(ca.major_cell_type, ' / ', mg.group_name)
                    ELSE mg.group_name
                END AS display_context,
                COUNT(*) AS record_count,
                MIN(mg.fdr) AS min_fdr,
                MAX(mg.avg_log2fc) AS max_log2fc,
                MAX(mg.mean_diff) AS max_mean_diff
            FROM oscar_marker_gene mg
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mg.dataset_id
             AND ca.domain = mg.domain
             AND ca.cluster_label = mg.group_name
            WHERE mg.gene_symbol IS NOT NULL
              AND mg.gene_symbol <> ''
              AND mg.is_deleted = 0
              AND (mg.dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            GROUP BY
                mg.gene_symbol,
                mg.domain,
                COALESCE(mg.cluster_source, ''),
                COALESCE(mg.signal_type, ''),
                mg.dataset_id,
                mg.group_name,
                COALESCE(ca.major_cell_type, mg.group_name, 'Unknown'),
                CASE
                    WHEN ca.major_cell_type IS NOT NULL AND ca.major_cell_type <> ''
                    THEN CONCAT(ca.major_cell_type, ' / ', mg.group_name)
                    ELSE mg.group_name
                END
            """)
    int insertGeneMarkerSummary(@Param("datasetId") String datasetId);

    @Select("SELECT DISTINCT dataset_id FROM oscar_gene_marker_summary ORDER BY dataset_id")
    List<String> getCompletedGeneSummaryDatasetIds();

    // =========================================================================
    // gene_marker_summary post-refresh statistics
    // =========================================================================

    @Select("""
            SELECT COUNT(*)
            FROM oscar_gene_marker_summary
            WHERE (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countGeneMarkerSummary(@Param("datasetId") String datasetId);

    @Select("""
            SELECT COUNT(DISTINCT gene_symbol)
            FROM oscar_gene_marker_summary
            WHERE (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            """)
    long countDistinctGenesInSummary(@Param("datasetId") String datasetId);

    @Select("""
            SELECT
                domain,
                COUNT(*) AS cnt
            FROM oscar_gene_marker_summary
            WHERE (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            GROUP BY domain
            """)
    List<Map<String, Object>> getSummaryDomainCounts(@Param("datasetId") String datasetId);

    @Select("""
            SELECT
                COALESCE(signal_type, '') AS signal_type,
                COUNT(*) AS cnt
            FROM oscar_gene_marker_summary
            WHERE (dataset_id = #{datasetId} OR #{datasetId} IS NULL)
            GROUP BY signal_type
            """)
    List<Map<String, Object>> getSummarySignalTypeCounts(@Param("datasetId") String datasetId);

    // =========================================================================
    // cell-enrichment post-summary statistics
    // =========================================================================

    @Delete("""
            DELETE FROM oscar_gene_marker_enrichment_universe
            WHERE scope_dataset_id = ''
            """)
    int deleteGlobalEnrichmentUniverses();

    @Delete("""
            DELETE FROM oscar_gene_marker_enrichment_set
            WHERE scope_dataset_id = ''
              AND result_level = 'cell_type'
            """)
    int deleteGlobalCellTypeEnrichmentSets();

    @Insert("""
            <script>
            INSERT INTO oscar_gene_marker_enrichment_universe (
                marker_reference,
                scope_dataset_id,
                universe_size
            )
            SELECT
                #{markerReference},
                '',
                COUNT(DISTINCT gms.gene_symbol)
            FROM oscar_gene_marker_summary gms
            WHERE gms.domain = #{domain}
              AND gms.signal_type IN
              <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
            ON DUPLICATE KEY UPDATE
                universe_size = VALUES(universe_size),
                updated_at = CURRENT_TIMESTAMP
            </script>
            """)
    int upsertGlobalEnrichmentUniverse(
            @Param("markerReference") String markerReference,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

    @Insert("""
            <script>
            INSERT INTO oscar_gene_marker_enrichment_set (
                marker_reference,
                scope_dataset_id,
                result_level,
                cell_type,
                context_label,
                context_hash,
                set_size,
                dataset_count
            )
            SELECT
                #{markerReference},
                '',
                'cell_type',
                COALESCE(NULLIF(grouped.cell_type, ''), 'Unknown'),
                COALESCE(NULLIF(grouped.cell_type, ''), 'Unknown'),
                MD5(COALESCE(NULLIF(grouped.cell_type, ''), 'Unknown')),
                grouped.set_size,
                grouped.dataset_count
            FROM (
                SELECT
                    COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown') AS cell_type,
                    COUNT(DISTINCT gms.gene_symbol) AS set_size,
                    COUNT(DISTINCT gms.dataset_id) AS dataset_count
                FROM oscar_gene_marker_summary gms FORCE INDEX (idx_gene_summary_enrich_celltype_gene_fast)
                WHERE gms.domain = #{domain}
                  AND gms.signal_type IN
                  <foreach item='signalType' collection='signalTypes' open='(' separator=',' close=')'>#{signalType}</foreach>
                GROUP BY COALESCE(NULLIF(TRIM(gms.major_cell_type), ''), 'Unknown')
            ) grouped
            ON DUPLICATE KEY UPDATE
                set_size = VALUES(set_size),
                dataset_count = VALUES(dataset_count),
                updated_at = CURRENT_TIMESTAMP
            </script>
            """)
    int upsertGlobalCellTypeEnrichmentSets(
            @Param("markerReference") String markerReference,
            @Param("domain") String domain,
            @Param("signalTypes") List<String> signalTypes
    );

}
