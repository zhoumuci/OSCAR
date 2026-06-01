package com.oscar.backend.mapper;

import com.oscar.backend.entity.BedtoolsMarkerPeakTrackRow;
import com.oscar.backend.entity.BedtoolsP2gTrackRow;
import com.oscar.backend.entity.BedtoolsTrackBundle;
import com.oscar.backend.entity.BedtoolsTrackItem;
import com.oscar.backend.entity.BedtoolsTrackSourceSummary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BedtoolsTrackMapper {

    @Select("""
            SELECT *
            FROM oscar_bedtools_track_bundle
            WHERE dataset_id = #{datasetId}
              AND data_domain = #{domain}
              AND genome_build = #{genomeBuild}
            LIMIT 1
            """)
    BedtoolsTrackBundle selectBundle(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("genomeBuild") String genomeBuild
    );

    @Insert("""
            INSERT INTO oscar_bedtools_track_bundle (
                dataset_id,
                data_domain,
                genome_build,
                status,
                manifest_path,
                source_fingerprint,
                reference_version,
                started_at,
                finished_at,
                generated_at,
                error_message
            ) VALUES (
                #{datasetId},
                #{dataDomain},
                #{genomeBuild},
                'BUILDING',
                NULL,
                #{sourceFingerprint},
                #{referenceVersion},
                NOW(),
                NULL,
                NULL,
                NULL
            )
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                status = 'BUILDING',
                manifest_path = NULL,
                source_fingerprint = #{sourceFingerprint},
                reference_version = #{referenceVersion},
                started_at = NOW(),
                finished_at = NULL,
                generated_at = NULL,
                error_message = NULL
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int upsertBundleBuilding(BedtoolsTrackBundle bundle);

    @Update("""
            UPDATE oscar_bedtools_track_bundle
            SET status = 'READY',
                manifest_path = #{manifestPath},
                source_fingerprint = #{sourceFingerprint},
                reference_version = #{referenceVersion},
                finished_at = NOW(),
                generated_at = #{generatedAt},
                error_message = NULL
            WHERE id = #{bundleId}
            """)
    int updateBundleReady(
            @Param("bundleId") Long bundleId,
            @Param("manifestPath") String manifestPath,
            @Param("sourceFingerprint") String sourceFingerprint,
            @Param("referenceVersion") String referenceVersion,
            @Param("generatedAt") LocalDateTime generatedAt
    );

    @Update("""
            UPDATE oscar_bedtools_track_bundle
            SET status = 'FAILED',
                finished_at = NOW(),
                error_message = #{errorMessage}
            WHERE id = #{bundleId}
            """)
    int updateBundleFailed(
            @Param("bundleId") Long bundleId,
            @Param("errorMessage") String errorMessage
    );

    @Select("""
            SELECT *
            FROM oscar_bedtools_track_item
            WHERE bundle_id = #{bundleId}
            ORDER BY FIELD(track_type, 'marker_peak', 'p2g_link'), track_type
            """)
    List<BedtoolsTrackItem> selectItems(@Param("bundleId") Long bundleId);

    @Insert("""
            INSERT INTO oscar_bedtools_track_item (
                bundle_id,
                track_type,
                status,
                track_path,
                record_count,
                skipped_count,
                error_message
            ) VALUES (
                #{bundleId},
                #{trackType},
                'BUILDING',
                NULL,
                0,
                0,
                NULL
            )
            ON DUPLICATE KEY UPDATE
                status = 'BUILDING',
                track_path = NULL,
                record_count = 0,
                skipped_count = 0,
                error_message = NULL
            """)
    int upsertTrackItemBuilding(
            @Param("bundleId") Long bundleId,
            @Param("trackType") String trackType
    );

    @Update("""
            UPDATE oscar_bedtools_track_item
            SET status = 'READY',
                track_path = #{trackPath},
                record_count = #{recordCount},
                skipped_count = #{skippedCount},
                error_message = NULL
            WHERE bundle_id = #{bundleId}
              AND track_type = #{trackType}
            """)
    int updateTrackItemReady(
            @Param("bundleId") Long bundleId,
            @Param("trackType") String trackType,
            @Param("trackPath") String trackPath,
            @Param("recordCount") long recordCount,
            @Param("skippedCount") long skippedCount
    );

    @Update("""
            UPDATE oscar_bedtools_track_item
            SET status = 'FAILED',
                error_message = #{errorMessage}
            WHERE bundle_id = #{bundleId}
              AND status = 'BUILDING'
            """)
    int updateBuildingTrackItemsFailed(
            @Param("bundleId") Long bundleId,
            @Param("errorMessage") String errorMessage
    );

    @Select("""
            SELECT dataset_id
            FROM (
                SELECT s.dataset_id
                FROM oscar_sample s
                WHERE (s.is_deleted IS NULL OR s.is_deleted = 0)
                  AND (s.is_visible IS NULL OR s.is_visible = 1)
                UNION
                SELECT DISTINCT mp.dataset_id
                FROM oscar_marker_peak mp
                WHERE mp.domain = #{domain}
                UNION
                SELECT DISTINCT mlr.dataset_id
                FROM oscar_marker_linked_region mlr
                WHERE mlr.domain = #{domain}
            ) dataset_ids
            ORDER BY dataset_id
            """)
    List<String> selectDatasetIdsForTrackBuild(@Param("domain") String domain);

    @Select("""
            SELECT
                COUNT(*) AS recordCount,
                MAX(updated_at) AS maxUpdatedAt
            FROM oscar_marker_peak
            WHERE dataset_id = #{datasetId}
              AND domain = #{domain}
            """)
    BedtoolsTrackSourceSummary selectMarkerPeakSourceSummary(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                COUNT(*) AS recordCount,
                MAX(updated_at) AS maxUpdatedAt
            FROM oscar_marker_linked_region
            WHERE dataset_id = #{datasetId}
              AND domain = #{domain}
            """)
    BedtoolsTrackSourceSummary selectP2gSourceSummary(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                mp.id AS id,
                NULLIF(TRIM(mp.chromosome), '') AS chromosome,
                mp.peak_start AS peakStart,
                mp.peak_end AS peakEnd,
                NULLIF(TRIM(mp.peak_name), '') AS peakName,
                NULLIF(TRIM(mp.group_name), '') AS groupName,
                NULLIF(TRIM(ca.major_cell_type), '') AS cellType,
                CAST(NULL AS CHAR) AS linkedGene,
                mp.log2fc AS log2fc,
                mp.fdr AS fdr
            FROM oscar_marker_peak mp
            LEFT JOIN oscar_cluster_annotation ca
              ON ca.dataset_id = mp.dataset_id
             AND ca.domain = mp.domain
             AND ca.cluster_label = mp.group_name
            WHERE mp.dataset_id = #{datasetId}
              AND mp.domain = #{domain}
            ORDER BY mp.chromosome ASC, mp.peak_start ASC, mp.peak_end ASC, mp.id ASC
            """)
    List<BedtoolsMarkerPeakTrackRow> selectMarkerPeakTrackRows(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select("""
            SELECT
                mlr.id AS id,
                NULLIF(TRIM(mlr.peak_chromosome), '') AS chromosome,
                mlr.peak_start AS peakStart,
                mlr.peak_end AS peakEnd,
                NULLIF(TRIM(mlr.gene_symbol), '') AS targetGene,
                NULLIF(TRIM(mlr.peak_region), '') AS linkedPeakRegion,
                NULLIF(TRIM(mlr.cell_type), '') AS cellType,
                NULLIF(TRIM(mlr.group_name), '') AS cluster,
                mlr.link_score AS p2gScore
            FROM oscar_marker_linked_region mlr
            WHERE mlr.dataset_id = #{datasetId}
              AND mlr.domain = #{domain}
            ORDER BY mlr.peak_chromosome ASC, mlr.peak_start ASC, mlr.peak_end ASC, mlr.id ASC
            """)
    List<BedtoolsP2gTrackRow> selectP2gTrackRows(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain
    );

    @Select({
            "<script>",
            "SELECT",
            "    mp.id AS id,",
            "    NULLIF(TRIM(mp.chromosome), '') AS chromosome,",
            "    mp.peak_start AS peakStart,",
            "    mp.peak_end AS peakEnd,",
            "    NULLIF(TRIM(mp.peak_name), '') AS peakName,",
            "    NULLIF(TRIM(mp.group_name), '') AS groupName,",
            "    NULLIF(TRIM(ca.major_cell_type), '') AS cellType,",
            "    linked_genes.linkedGene AS linkedGene,",
            "    mp.log2fc AS log2fc,",
            "    mp.fdr AS fdr",
            "FROM oscar_marker_peak mp",
            "LEFT JOIN oscar_cluster_annotation ca",
            "  ON ca.dataset_id = mp.dataset_id",
            " AND ca.domain = mp.domain",
            " AND ca.cluster_label = mp.group_name",
            "LEFT JOIN (",
            "    SELECT",
            "        pgl.dataset_id,",
            "        pgl.domain,",
            "        pgl.chromosome,",
            "        pgl.peak_start,",
            "        pgl.peak_end,",
            "        GROUP_CONCAT(DISTINCT NULLIF(TRIM(pgl.gene_name), '') ORDER BY pgl.gene_name SEPARATOR ', ') AS linkedGene",
            "    FROM oscar_peak_gene_link pgl",
            "    WHERE pgl.dataset_id = #{datasetId}",
            "      AND pgl.domain = #{domain}",
            "      AND (pgl.is_deleted IS NULL OR pgl.is_deleted = 0)",
            "    GROUP BY pgl.dataset_id, pgl.domain, pgl.chromosome, pgl.peak_start, pgl.peak_end",
            ") linked_genes",
            "  ON linked_genes.dataset_id = mp.dataset_id",
            " AND linked_genes.domain = mp.domain",
            " AND linked_genes.chromosome = mp.chromosome",
            " AND linked_genes.peak_start = mp.peak_start",
            " AND linked_genes.peak_end = mp.peak_end",
            "WHERE mp.dataset_id = #{datasetId}",
            "  AND mp.domain = #{domain}",
            "  AND mp.id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "</foreach>",
            "</script>"
    })
    @MapKey("id")
    Map<Long, BedtoolsMarkerPeakTrackRow> selectMarkerPeakHydrationRows(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("ids") List<Long> ids
    );

    @Select({
            "<script>",
            "SELECT",
            "    mlr.id AS id,",
            "    NULLIF(TRIM(mlr.peak_chromosome), '') AS chromosome,",
            "    mlr.peak_start AS peakStart,",
            "    mlr.peak_end AS peakEnd,",
            "    NULLIF(TRIM(mlr.gene_symbol), '') AS targetGene,",
            "    NULLIF(TRIM(mlr.peak_region), '') AS linkedPeakRegion,",
            "    NULLIF(TRIM(mlr.cell_type), '') AS cellType,",
            "    NULLIF(TRIM(mlr.group_name), '') AS cluster,",
            "    mlr.link_score AS p2gScore",
            "FROM oscar_marker_linked_region mlr",
            "WHERE mlr.dataset_id = #{datasetId}",
            "  AND mlr.domain = #{domain}",
            "  AND mlr.id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "</foreach>",
            "</script>"
    })
    @MapKey("id")
    Map<Long, BedtoolsP2gTrackRow> selectP2gHydrationRows(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("ids") List<Long> ids
    );
}
