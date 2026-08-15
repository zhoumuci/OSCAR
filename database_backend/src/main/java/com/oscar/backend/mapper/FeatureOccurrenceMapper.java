package com.oscar.backend.mapper;

import com.oscar.backend.entity.FeatureOccurrenceResponse.CellContextRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.DatasetRankingItem;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceDatasetEntry;
import com.oscar.backend.entity.FeatureOccurrenceResponse.FeatureOccurrenceTopCellType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
@Mapper
public interface FeatureOccurrenceMapper {

    // ===== gene overview: one summary-table scan, aggregated in the service =====
    @Select("""
            SELECT
                gms.dataset_id AS datasetId,
                COALESCE(NULLIF(TRIM(s.sample_name),''), gms.dataset_id) AS sampleName,
                NULLIF(TRIM(gms.major_cell_type), '') AS cellType,
                NULLIF(TRIM(gms.group_name), '') AS cluster,
                SUM(gms.record_count) AS recordCount
            FROM oscar_gene_marker_summary gms
            LEFT JOIN oscar_sample s ON s.dataset_id = gms.dataset_id
              AND (s.is_deleted IS NULL OR s.is_deleted = 0)
              AND (s.is_visible IS NULL OR s.is_visible = 1)
            WHERE gms.gene_symbol = #{gene}
              AND gms.domain = #{domain}
            GROUP BY gms.dataset_id, s.sample_name, gms.major_cell_type, gms.group_name
            ORDER BY gms.dataset_id ASC, gms.major_cell_type ASC, gms.group_name ASC
            """)
    List<GeneOccurrenceContextRow> selectGeneOccurrenceContexts(
            @Param("gene") String gene,
            @Param("domain") String domain
    );

    // ===== peak top cell types =====
    @Select("SELECT NULLIF(TRIM(ca.major_cell_type),'') AS cellType, COUNT(DISTINCT mp.id) AS count FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY count DESC LIMIT #{limit}")
    List<FeatureOccurrenceTopCellType> selectPeakTopCellTypes(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

    @Select("SELECT mp.dataset_id AS datasetId, NULLIF(TRIM(ca.major_cell_type),'') AS cellType, NULLIF(TRIM(mp.group_name),'') AS cluster, COUNT(DISTINCT mp.id) AS occurrenceCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY mp.dataset_id, ca.major_cell_type, mp.group_name ORDER BY mp.dataset_id ASC, ca.major_cell_type ASC, mp.group_name ASC LIMIT #{limit}")
    List<FeatureOccurrenceDatasetEntry> selectPeakDatasetEntries(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

    @Select("SELECT COUNT(DISTINCT mp.id) AS total, COUNT(DISTINCT mp.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellTypeCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain}")
    FeatureOccurrenceAggregation selectPeakAggregation(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain);

    // ===== peak ranking =====
    @Select("SELECT mp.dataset_id AS datasetId, COALESCE(NULLIF(TRIM(s.sample_name),''), mp.dataset_id) AS sampleName, COUNT(*) AS recordCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellContextCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name LEFT JOIN oscar_sample s ON s.dataset_id=mp.dataset_id AND (s.is_deleted IS NULL OR s.is_deleted=0) AND (s.is_visible IS NULL OR s.is_visible=1) WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY mp.dataset_id ORDER BY recordCount DESC, mp.dataset_id ASC LIMIT #{limit}")
    List<DatasetRankingItem> selectPeakDatasetRanking(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

    @Select("SELECT COALESCE(NULLIF(TRIM(ca.major_cell_type),''), 'Unknown') AS cellType, COUNT(*) AS recordCount, COUNT(DISTINCT mp.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY recordCount DESC, cellType ASC LIMIT #{limit}")
    List<CellContextRankingItem> selectPeakCellContextRanking(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

    @Select("""
            SELECT
                NULLIF(TRIM(mg.chromosome), '') AS chromosome,
                mg.gene_start AS start,
                mg.gene_end AS end,
                NULLIF(TRIM(mg.strand), '') AS strand
            FROM oscar_marker_gene mg
            WHERE mg.gene_symbol = #{gene}
              AND mg.domain = #{domain}
              AND mg.chromosome IS NOT NULL
              AND TRIM(mg.chromosome) <> ''
              AND mg.gene_start IS NOT NULL
              AND mg.gene_end IS NOT NULL
            LIMIT 1
            """)
    GeneRegion selectGeneRegionFromMarkerGene(@Param("gene") String gene, @Param("domain") String domain);

    @Select("""
            SELECT
                NULLIF(TRIM(mlr.gene_chromosome), '') AS chromosome,
                mlr.gene_start AS start,
                mlr.gene_end AS end,
                NULLIF(TRIM(mlr.strand), '') AS strand
            FROM oscar_marker_linked_region mlr
            WHERE mlr.gene_symbol = #{gene}
              AND mlr.domain = #{domain}
              AND mlr.gene_chromosome IS NOT NULL
              AND TRIM(mlr.gene_chromosome) <> ''
              AND mlr.gene_start IS NOT NULL
              AND mlr.gene_end IS NOT NULL
            LIMIT 1
            """)
    GeneRegion selectGeneRegionFromLinkedRegion(@Param("gene") String gene, @Param("domain") String domain);

    @Select("""
            SELECT
                id,
                NULLIF(TRIM(gene_symbol), '') AS geneSymbol,
                NULLIF(TRIM(chromosome), '') AS chromosome,
                region_start AS regionStart,
                region_end AS regionEnd,
                UPPER(TRIM(enhancer_type)) AS enhancerType,
                NULLIF(TRIM(biosample_name), '') AS biosampleName
            FROM oscar_gene_enhancer
            WHERE gene_symbol = #{gene}
              AND enhancer_type = #{enhancerType}
              AND chromosome IS NOT NULL
              AND TRIM(chromosome) <> ''
              AND region_start IS NOT NULL
              AND region_end IS NOT NULL
              AND region_end > region_start
            ORDER BY chromosome ASC, region_start ASC, region_end ASC, biosample_name ASC, id ASC
            """)
    List<GeneEnhancerRegion> selectAllGeneEnhancerRegions(
            @Param("gene") String gene,
            @Param("enhancerType") String enhancerType
    );

    @Select("""
            SELECT
                id,
                gene_symbol AS geneSymbol,
                chromosome,
                region_start AS regionStart,
                region_end AS regionEnd,
                enhancer_type AS enhancerType,
                biosample_name AS biosampleName
            FROM oscar_gene_enhancer
            WHERE gene_symbol = #{gene}
              AND enhancer_type = #{enhancerType}
            ORDER BY chromosome ASC, region_start ASC, region_end ASC, biosample_name ASC, id ASC
            """)
    List<Map<String, Object>> selectGeneEnhancerRegionMaps(
            @Param("gene") String gene,
            @Param("enhancerType") String enhancerType
    );

    @Select("""
            SELECT
                gene_symbol AS geneSymbol,
                platform,
                sample_name,
                expression_value
            FROM oscar_gene_expression
            WHERE gene_symbol = #{gene}
              AND platform = #{platform}
            ORDER BY expression_value DESC, sample_name ASC
            LIMIT 30
            """)
    List<Map<String, Object>> selectGeneExpressionTop30(
            @Param("gene") String gene,
            @Param("platform") String platform
    );

    @Select("""
            SELECT
                gene_symbol AS geneSymbol,
                platform,
                sample_name,
                expression_value
            FROM oscar_gene_expression
            WHERE gene_symbol = #{gene}
              AND platform = #{platform}
            ORDER BY expression_value DESC, sample_name ASC
            """)
    List<Map<String, Object>> selectAllGeneExpression(
            @Param("gene") String gene,
            @Param("platform") String platform
    );

    // ===== aggregation inner class =====
    public static class FeatureOccurrenceAggregation {
        private long total;
        private long datasetCount;
        private long cellTypeCount;
        private long clusterCount;
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getDatasetCount() { return datasetCount; }
        public void setDatasetCount(long datasetCount) { this.datasetCount = datasetCount; }
        public long getCellTypeCount() { return cellTypeCount; }
        public void setCellTypeCount(long cellTypeCount) { this.cellTypeCount = cellTypeCount; }
        public long getClusterCount() { return clusterCount; }
        public void setClusterCount(long clusterCount) { this.clusterCount = clusterCount; }
    }

    public static class GeneOccurrenceContextRow {
        private String datasetId;
        private String sampleName;
        private String cellType;
        private String cluster;
        private long recordCount;

        public String getDatasetId() { return datasetId; }
        public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
        public String getSampleName() { return sampleName; }
        public void setSampleName(String sampleName) { this.sampleName = sampleName; }
        public String getCellType() { return cellType; }
        public void setCellType(String cellType) { this.cellType = cellType; }
        public String getCluster() { return cluster; }
        public void setCluster(String cluster) { this.cluster = cluster; }
        public long getRecordCount() { return recordCount; }
        public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
    }

    public static class GeneRegion {
        private String chromosome;
        private Long start;
        private Long end;
        private String strand;

        public String getChromosome() { return chromosome; }
        public void setChromosome(String chromosome) { this.chromosome = chromosome; }
        public Long getStart() { return start; }
        public void setStart(Long start) { this.start = start; }
        public Long getEnd() { return end; }
        public void setEnd(Long end) { this.end = end; }
        public String getStrand() { return strand; }
        public void setStrand(String strand) { this.strand = strand; }
    }

    public static class GeneEnhancerRegion {
        private Long id;
        private String geneSymbol;
        private String chromosome;
        private Long regionStart;
        private Long regionEnd;
        private String enhancerType;
        private String biosampleName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getGeneSymbol() { return geneSymbol; }
        public void setGeneSymbol(String geneSymbol) { this.geneSymbol = geneSymbol; }
        public String getChromosome() { return chromosome; }
        public void setChromosome(String chromosome) { this.chromosome = chromosome; }
        public Long getRegionStart() { return regionStart; }
        public void setRegionStart(Long regionStart) { this.regionStart = regionStart; }
        public Long getRegionEnd() { return regionEnd; }
        public void setRegionEnd(Long regionEnd) { this.regionEnd = regionEnd; }
        public String getEnhancerType() { return enhancerType; }
        public void setEnhancerType(String enhancerType) { this.enhancerType = enhancerType; }
        public String getBiosampleName() { return biosampleName; }
        public void setBiosampleName(String biosampleName) { this.biosampleName = biosampleName; }
    }
}
