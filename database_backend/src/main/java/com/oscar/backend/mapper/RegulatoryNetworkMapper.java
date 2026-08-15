package com.oscar.backend.mapper;

import com.oscar.backend.entity.RegulatoryNetworkLink;
import com.oscar.backend.entity.RegulatoryNetworkSummaryRow;
import com.oscar.backend.entity.RegulatoryNetworkTopItemRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegulatoryNetworkMapper {

    String LINK_SELECT = """
            SELECT
                p.peak_name AS peak,
                p.peak_name AS peakId,
                p.gene_name AS geneSymbol,
                p.gene_name AS linkedGene,
                p.dataset_id AS datasetId,
                p.domain AS domain,
                COALESCE(NULLIF(TRIM(s.sample_name), ''), p.dataset_id) AS sampleName,
                p.link_score AS score,
                p.link_score AS linkScore,
                p.correlation,
                p.fdr,
                p.var_q_atac AS varQAtac,
                p.var_q_rna AS varQRna,
                CAST(NULL AS SIGNED) AS distanceToTss,
                'peak_to_gene' AS linkType,
                COALESCE(NULLIF(TRIM(s.sample_name), ''), p.dataset_id) AS source,
                NULLIF(TRIM(p.source), '') AS provenanceSource,
                NULLIF(TRIM(p.tf_name), '') AS tfName
            """;

    String LINK_FROM_AND_FILTER = """
            FROM oscar_peak_gene_link p
            LEFT JOIN oscar_sample s
              ON s.dataset_id = p.dataset_id
             AND (s.is_deleted IS NULL OR s.is_deleted = 0)
             AND (s.is_visible IS NULL OR s.is_visible = 1)
            WHERE p.dataset_id = #{datasetId}
              AND p.domain = #{domain}
              AND p.is_visible = 1
              AND p.is_deleted = 0
            """;

    String SUMMARY_FILTER = """
            p.dataset_id = #{datasetId}
            AND p.domain = #{domain}
            AND p.is_visible = 1
            AND p.is_deleted = 0
            <if test='minScore != null'>
              AND COALESCE(p.link_score, ABS(p.correlation), 0) >= #{minScore}
            </if>
            """;

    String LINK_SCORE_FILTER = """
            <if test='minScore != null'>
              AND COALESCE(p.link_score, ABS(p.correlation), 0) >= #{minScore}
            </if>
            """;

    String GENE_FILTER = """
            <if test='geneFilter != null'>
              AND p.gene_name = #{geneFilter}
            </if>
            """;

    String PEAK_FILTER = """
            <if test='peakFilter != null'>
              AND p.peak_name = #{peakFilter}
            </if>
            """;

    String LINK_ORDER = """
            ORDER BY COALESCE(p.link_score, ABS(p.correlation), 0) DESC,
                     COALESCE(p.fdr, 1) ASC,
                     p.id ASC
            """;

    String LINK_ORDER_AND_LIMIT = LINK_ORDER + """
            LIMIT #{limit}
            """;

    String LINK_ORDER_AND_PAGE = LINK_ORDER + """
            LIMIT #{limit} OFFSET #{offset}
            """;

    String LINK_COUNT_FROM_AND_FILTER = """
            FROM oscar_peak_gene_link p
            WHERE p.dataset_id = #{datasetId}
              AND p.domain = #{domain}
              AND p.is_visible = 1
              AND p.is_deleted = 0
            """;

    String BALANCED_TOP_GENE_BASE_FILTER = """
            p.dataset_id = #{datasetId}
            AND p.domain = #{domain}
            AND p.is_visible = 1
            AND p.is_deleted = 0
            <if test='minScore != null'>
              AND COALESCE(p.link_score, ABS(p.correlation), 0) >= #{minScore}
            </if>
            """;

    String BALANCED_LINK_BASE_FILTER = """
            p.dataset_id = #{datasetId}
            AND p.domain = #{domain}
            AND p.is_visible = 1
            AND p.is_deleted = 0
            <if test='minScore != null'>
              AND COALESCE(p.link_score, ABS(p.correlation), 0) >= #{minScore}
            </if>
            """;

    @Select({
            "<script>",
            LINK_SELECT,
            LINK_FROM_AND_FILTER,
            LINK_SCORE_FILTER,
            LINK_ORDER_AND_LIMIT,
            "</script>"
    })
    List<RegulatoryNetworkLink> selectOverviewLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("minScore") Double minScore,
            @Param("limit") Integer limit
    );

    @Select({
            "<script>",
            "WITH top_genes AS (",
            "  SELECT",
            "    gene_stats.gene_name,",
            "    ROW_NUMBER() OVER (",
            "      ORDER BY gene_stats.best_score DESC, gene_stats.best_fdr ASC, gene_stats.link_count DESC, gene_stats.gene_name ASC",
            "    ) AS gene_rank",
            "  FROM (",
            "    SELECT",
            "      p.gene_name,",
            "      MAX(COALESCE(p.link_score, ABS(p.correlation), 0)) AS best_score,",
            "      MIN(COALESCE(p.fdr, 1)) AS best_fdr,",
            "      COUNT(*) AS link_count",
            "    FROM oscar_peak_gene_link p",
            "    WHERE " + BALANCED_TOP_GENE_BASE_FILTER,
            "    GROUP BY p.gene_name",
            "    ORDER BY best_score DESC, best_fdr ASC, link_count DESC, p.gene_name ASC",
            "    LIMIT #{topGeneLimit}",
            "  ) gene_stats",
            "),",
            "ranked_links AS (",
            "  SELECT",
            "    p.*,",
            "    tg.gene_rank,",
            "    ROW_NUMBER() OVER (",
            "      PARTITION BY p.gene_name",
            "      ORDER BY COALESCE(p.link_score, ABS(p.correlation), 0) DESC, COALESCE(p.fdr, 1) ASC, p.id ASC",
            "    ) AS link_rank",
            "  FROM oscar_peak_gene_link p",
            "  JOIN top_genes tg ON tg.gene_name = p.gene_name",
            "  WHERE " + BALANCED_LINK_BASE_FILTER,
            ")",
            LINK_SELECT,
            "FROM ranked_links p",
            "LEFT JOIN oscar_sample s",
            "  ON s.dataset_id = p.dataset_id",
            " AND (s.is_deleted IS NULL OR s.is_deleted = 0)",
            " AND (s.is_visible IS NULL OR s.is_visible = 1)",
            "WHERE p.link_rank &lt;= #{perGenePeakLimit}",
            "ORDER BY p.gene_rank ASC, p.link_rank ASC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<RegulatoryNetworkLink> selectBalancedOverviewLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("minScore") Double minScore,
            @Param("topGeneLimit") Integer topGeneLimit,
            @Param("perGenePeakLimit") Integer perGenePeakLimit,
            @Param("limit") Integer limit
    );

    @Select({
            "<script>",
            LINK_SELECT,
            LINK_FROM_AND_FILTER,
            "  AND (",
            "      p.gene_name = #{gene}",
            "      <if test='allowPrefix'>",
            "      OR p.gene_name LIKE CONCAT(#{gene}, '%')",
            "      </if>",
            "  )",
            PEAK_FILTER,
            LINK_SCORE_FILTER,
            "ORDER BY",
            "  <if test='allowPrefix'>",
            "  CASE WHEN p.gene_name = #{gene} THEN 0 ELSE 1 END,",
            "  </if>",
            "  COALESCE(p.link_score, ABS(p.correlation), 0) DESC,",
            "  COALESCE(p.fdr, 1) ASC,",
            "  p.id ASC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<RegulatoryNetworkLink> selectGeneLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("gene") String gene,
            @Param("allowPrefix") boolean allowPrefix,
            @Param("peakFilter") String peakFilter,
            @Param("minScore") Double minScore,
            @Param("limit") Integer limit
    );

    @Select({
            "<script>",
            LINK_SELECT,
            LINK_FROM_AND_FILTER,
            "  AND p.peak_name = #{peak}",
            GENE_FILTER,
            LINK_SCORE_FILTER,
            LINK_ORDER_AND_LIMIT,
            "</script>"
    })
    List<RegulatoryNetworkLink> selectPeakLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peak") String peak,
            @Param("geneFilter") String geneFilter,
            @Param("minScore") Double minScore,
            @Param("limit") Integer limit
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            LINK_COUNT_FROM_AND_FILTER,
            "  AND p.gene_name = #{gene}",
            PEAK_FILTER,
            LINK_SCORE_FILTER,
            "</script>"
    })
    Long countGeneLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("gene") String gene,
            @Param("peakFilter") String peakFilter,
            @Param("minScore") Double minScore
    );

    @Select({
            "<script>",
            LINK_SELECT,
            LINK_FROM_AND_FILTER,
            "  AND p.gene_name = #{gene}",
            PEAK_FILTER,
            LINK_SCORE_FILTER,
            LINK_ORDER_AND_PAGE,
            "</script>"
    })
    List<RegulatoryNetworkLink> selectGeneLinksPage(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("gene") String gene,
            @Param("peakFilter") String peakFilter,
            @Param("minScore") Double minScore,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            LINK_COUNT_FROM_AND_FILTER,
            "  AND p.peak_name = #{peak}",
            GENE_FILTER,
            LINK_SCORE_FILTER,
            "</script>"
    })
    Long countPeakLinks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peak") String peak,
            @Param("geneFilter") String geneFilter,
            @Param("minScore") Double minScore
    );

    @Select({
            "<script>",
            LINK_SELECT,
            LINK_FROM_AND_FILTER,
            "  AND p.peak_name = #{peak}",
            GENE_FILTER,
            LINK_SCORE_FILTER,
            LINK_ORDER_AND_PAGE,
            "</script>"
    })
    List<RegulatoryNetworkLink> selectPeakLinksPage(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peak") String peak,
            @Param("geneFilter") String geneFilter,
            @Param("minScore") Double minScore,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select({
            "<script>",
            "SELECT",
            "  p.gene_name AS nodeKey,",
            "  COUNT(DISTINCT p.peak_name) AS linkedCount,",
            "  MAX(p.link_score) AS maxLinkScore,",
            "  MIN(p.correlation) AS minCorrelation,",
            "  MAX(p.correlation) AS maxCorrelation,",
            "  MIN(p.fdr) AS minFdr,",
            "  MAX(p.fdr) AS maxFdr",
            "FROM oscar_peak_gene_link p",
            "WHERE " + SUMMARY_FILTER,
            "  AND p.gene_name IN",
            "  <foreach collection='genes' item='gene' open='(' separator=',' close=')'>",
            "    #{gene}",
            "  </foreach>",
            PEAK_FILTER,
            "GROUP BY p.gene_name",
            "</script>"
    })
    List<RegulatoryNetworkSummaryRow> selectGeneSummaries(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("genes") List<String> genes,
            @Param("peakFilter") String peakFilter,
            @Param("minScore") Double minScore
    );

    @Select({
            "<script>",
            "SELECT ranked.nodeKey, ranked.item",
            "FROM (",
            "  SELECT",
            "    per_item.nodeKey,",
            "    per_item.item,",
            "    ROW_NUMBER() OVER (",
            "      PARTITION BY per_item.nodeKey",
            "      ORDER BY per_item.maxLinkScore DESC, per_item.minFdr ASC, per_item.minId ASC",
            "    ) AS rowNumber",
            "  FROM (",
            "    SELECT",
            "      p.gene_name AS nodeKey,",
            "      p.peak_name AS item,",
            "      MAX(p.link_score) AS maxLinkScore,",
            "      MIN(p.fdr) AS minFdr,",
            "      MIN(p.id) AS minId",
            "    FROM oscar_peak_gene_link p",
            "    WHERE " + SUMMARY_FILTER,
            "      AND p.gene_name IN",
            "      <foreach collection='genes' item='gene' open='(' separator=',' close=')'>",
            "        #{gene}",
            "      </foreach>",
            PEAK_FILTER,
            "    GROUP BY p.gene_name, p.peak_name",
            "  ) per_item",
            ") ranked",
            "WHERE ranked.rowNumber &lt;= 3",
            "ORDER BY ranked.nodeKey ASC, ranked.rowNumber ASC",
            "</script>"
    })
    List<RegulatoryNetworkTopItemRow> selectTopPeaksForGenes(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("genes") List<String> genes,
            @Param("peakFilter") String peakFilter,
            @Param("minScore") Double minScore
    );

    @Select({
            "<script>",
            "SELECT",
            "  p.peak_name AS nodeKey,",
            "  COUNT(DISTINCT p.gene_name) AS linkedCount,",
            "  MAX(p.link_score) AS maxLinkScore,",
            "  MIN(p.correlation) AS minCorrelation,",
            "  MAX(p.correlation) AS maxCorrelation,",
            "  MIN(p.fdr) AS minFdr,",
            "  MAX(p.fdr) AS maxFdr",
            "FROM oscar_peak_gene_link p",
            "WHERE " + SUMMARY_FILTER,
            "  AND p.peak_name IN",
            "  <foreach collection='peaks' item='peak' open='(' separator=',' close=')'>",
            "    #{peak}",
            "  </foreach>",
            GENE_FILTER,
            "GROUP BY p.peak_name",
            "</script>"
    })
    List<RegulatoryNetworkSummaryRow> selectPeakSummaries(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peaks") List<String> peaks,
            @Param("geneFilter") String geneFilter,
            @Param("minScore") Double minScore
    );

    @Select({
            "<script>",
            "SELECT ranked.nodeKey, ranked.item",
            "FROM (",
            "  SELECT",
            "    per_item.nodeKey,",
            "    per_item.item,",
            "    ROW_NUMBER() OVER (",
            "      PARTITION BY per_item.nodeKey",
            "      ORDER BY per_item.maxLinkScore DESC, per_item.minFdr ASC, per_item.minId ASC",
            "    ) AS rowNumber",
            "  FROM (",
            "    SELECT",
            "      p.peak_name AS nodeKey,",
            "      p.gene_name AS item,",
            "      MAX(p.link_score) AS maxLinkScore,",
            "      MIN(p.fdr) AS minFdr,",
            "      MIN(p.id) AS minId",
            "    FROM oscar_peak_gene_link p",
            "    WHERE " + SUMMARY_FILTER,
            "      AND p.peak_name IN",
            "      <foreach collection='peaks' item='peak' open='(' separator=',' close=')'>",
            "        #{peak}",
            "      </foreach>",
            GENE_FILTER,
            "    GROUP BY p.peak_name, p.gene_name",
            "  ) per_item",
            ") ranked",
            "WHERE ranked.rowNumber &lt;= 3",
            "ORDER BY ranked.nodeKey ASC, ranked.rowNumber ASC",
            "</script>"
    })
    List<RegulatoryNetworkTopItemRow> selectTopGenesForPeaks(
            @Param("datasetId") String datasetId,
            @Param("domain") String domain,
            @Param("peaks") List<String> peaks,
            @Param("geneFilter") String geneFilter,
            @Param("minScore") Double minScore
    );
}
