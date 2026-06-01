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
@Mapper
public interface FeatureOccurrenceMapper {

    // ===== gene top cell types =====
    @Select("SELECT NULLIF(TRIM(ca.major_cell_type),'') AS cellType, COUNT(DISTINCT mg.id) AS count FROM oscar_marker_gene mg LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mg.dataset_id AND ca.domain=mg.domain AND ca.cluster_label=mg.group_name WHERE UPPER(TRIM(mg.gene_symbol))=#{gene} AND mg.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY count DESC LIMIT 10")
    List<FeatureOccurrenceTopCellType> selectGeneTopCellTypes(@Param("gene") String gene, @Param("domain") String domain);

    @Select("SELECT mg.dataset_id AS datasetId, NULLIF(TRIM(ca.major_cell_type),'') AS cellType, NULLIF(TRIM(mg.group_name),'') AS cluster, COUNT(DISTINCT mg.id) AS occurrenceCount FROM oscar_marker_gene mg LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mg.dataset_id AND ca.domain=mg.domain AND ca.cluster_label=mg.group_name WHERE UPPER(TRIM(mg.gene_symbol))=#{gene} AND mg.domain=#{domain} GROUP BY mg.dataset_id, ca.major_cell_type, mg.group_name ORDER BY mg.dataset_id ASC, ca.major_cell_type ASC, mg.group_name ASC LIMIT 100")
    List<FeatureOccurrenceDatasetEntry> selectGeneDatasetEntries(@Param("gene") String gene, @Param("domain") String domain);

    @Select("SELECT COUNT(DISTINCT mg.id) AS total, COUNT(DISTINCT mg.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellTypeCount, COUNT(DISTINCT NULLIF(TRIM(mg.group_name),'')) AS clusterCount FROM oscar_marker_gene mg LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mg.dataset_id AND ca.domain=mg.domain AND ca.cluster_label=mg.group_name WHERE UPPER(TRIM(mg.gene_symbol))=#{gene} AND mg.domain=#{domain}")
    FeatureOccurrenceAggregation selectGeneAggregation(@Param("gene") String gene, @Param("domain") String domain);

    // ===== gene ranking =====
    @Select("SELECT mg.dataset_id AS datasetId, COUNT(*) AS recordCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellContextCount, COUNT(DISTINCT NULLIF(TRIM(mg.group_name),'')) AS clusterCount FROM oscar_marker_gene mg LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mg.dataset_id AND ca.domain=mg.domain AND ca.cluster_label=mg.group_name WHERE UPPER(TRIM(mg.gene_symbol))=#{gene} AND mg.domain=#{domain} GROUP BY mg.dataset_id ORDER BY recordCount DESC, mg.dataset_id ASC LIMIT #{limit}")
    List<DatasetRankingItem> selectGeneDatasetRanking(@Param("gene") String gene, @Param("domain") String domain, @Param("limit") int limit);

    @Select("SELECT COALESCE(NULLIF(TRIM(ca.major_cell_type),''), 'Unknown') AS cellType, COUNT(*) AS recordCount, COUNT(DISTINCT mg.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(mg.group_name),'')) AS clusterCount FROM oscar_marker_gene mg LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mg.dataset_id AND ca.domain=mg.domain AND ca.cluster_label=mg.group_name WHERE UPPER(TRIM(mg.gene_symbol))=#{gene} AND mg.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY recordCount DESC, cellType ASC LIMIT #{limit}")
    List<CellContextRankingItem> selectGeneCellContextRanking(@Param("gene") String gene, @Param("domain") String domain, @Param("limit") int limit);

    // ===== peak top cell types =====
    @Select("SELECT NULLIF(TRIM(ca.major_cell_type),'') AS cellType, COUNT(DISTINCT mp.id) AS count FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY count DESC LIMIT 10")
    List<FeatureOccurrenceTopCellType> selectPeakTopCellTypes(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain);

    @Select("SELECT mp.dataset_id AS datasetId, NULLIF(TRIM(ca.major_cell_type),'') AS cellType, NULLIF(TRIM(mp.group_name),'') AS cluster, COUNT(DISTINCT mp.id) AS occurrenceCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY mp.dataset_id, ca.major_cell_type, mp.group_name ORDER BY mp.dataset_id ASC, ca.major_cell_type ASC, mp.group_name ASC LIMIT 100")
    List<FeatureOccurrenceDatasetEntry> selectPeakDatasetEntries(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain);

    @Select("SELECT COUNT(DISTINCT mp.id) AS total, COUNT(DISTINCT mp.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellTypeCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain}")
    FeatureOccurrenceAggregation selectPeakAggregation(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain);

    // ===== peak ranking =====
    @Select("SELECT mp.dataset_id AS datasetId, COUNT(*) AS recordCount, COUNT(DISTINCT NULLIF(TRIM(ca.major_cell_type),'')) AS cellContextCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY mp.dataset_id ORDER BY recordCount DESC, mp.dataset_id ASC LIMIT #{limit}")
    List<DatasetRankingItem> selectPeakDatasetRanking(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

    @Select("SELECT COALESCE(NULLIF(TRIM(ca.major_cell_type),''), 'Unknown') AS cellType, COUNT(*) AS recordCount, COUNT(DISTINCT mp.dataset_id) AS datasetCount, COUNT(DISTINCT NULLIF(TRIM(mp.group_name),'')) AS clusterCount FROM oscar_marker_peak mp LEFT JOIN oscar_cluster_annotation ca ON ca.dataset_id=mp.dataset_id AND ca.domain=mp.domain AND ca.cluster_label=mp.group_name WHERE mp.chromosome=#{chrom} AND mp.peak_start=#{start} AND mp.peak_end=#{end} AND mp.domain=#{domain} GROUP BY ca.major_cell_type ORDER BY recordCount DESC, cellType ASC LIMIT #{limit}")
    List<CellContextRankingItem> selectPeakCellContextRanking(@Param("chrom") String chrom, @Param("start") long start, @Param("end") long end, @Param("domain") String domain, @Param("limit") int limit);

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
}
