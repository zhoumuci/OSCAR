package com.oscar.backend.mapper;

import com.oscar.backend.entity.BrowseFacetItemResponse;
import com.oscar.backend.entity.BrowseSampleQuery;
import com.oscar.backend.entity.BrowseSampleResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrowseMapper {

    String BROWSE_FILTERS = """
            <if test='species != null'>
            AND species = #{species}
            </if>
            <if test='sampleType != null'>
            AND sample_type = #{sampleType}
            </if>
            <if test='tissue != null'>
            AND tissue = #{tissue}
            </if>
            <if test='keyword != null'>
            AND (
                dataset_id LIKE CONCAT('%', #{keyword}, '%')
                OR sample_type LIKE CONCAT('%', #{keyword}, '%')
                OR tissue LIKE CONCAT('%', #{keyword}, '%')
                OR sample_name LIKE CONCAT('%', #{keyword}, '%')
                OR CAST(cell_count AS CHAR) LIKE CONCAT('%', #{keyword}, '%')
                OR platform LIKE CONCAT('%', #{keyword}, '%')
                OR source_id LIKE CONCAT('%', #{keyword}, '%')
                OR disease LIKE CONCAT('%', #{keyword}, '%')
                OR sample_source LIKE CONCAT('%', #{keyword}, '%')
            )
            </if>
            """;

    /*
     * Browse is a sample-level page. These queries intentionally use oscar_sample
     * only; oscar_cell_profile is cell-level data for future detail/embedding pages.
     * dataset_id is exposed as datasetId because it is the business sample key.
     */
    @Select({
            "<script>",
            "SELECT",
            "dataset_id AS datasetId,",
            "sample_type AS sampleType,",
            "tissue,",
            "sample_name AS sampleName,",
            "cell_count AS cells,",
            "platform,",
            "source_id AS sourceId,",
            "disease,",
            "sample_source AS sampleSource",
            "FROM oscar_sample",
            "WHERE (is_deleted IS NULL OR is_deleted = 0)",
            "AND (is_visible IS NULL OR is_visible = 1)",
            BROWSE_FILTERS,
            "ORDER BY ${sortColumn} ${sortDirection}",
            "LIMIT #{pageSize} OFFSET #{offset}",
            "</script>"
    })
    List<BrowseSampleResponse> selectSamples(BrowseSampleQuery query);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM oscar_sample",
            "WHERE (is_deleted IS NULL OR is_deleted = 0)",
            "AND (is_visible IS NULL OR is_visible = 1)",
            BROWSE_FILTERS,
            "</script>"
    })
    long countSamples(BrowseSampleQuery query);

    @Select({
            "<script>",
            "SELECT TRIM(species) AS label, COUNT(*) AS count",
            "FROM oscar_sample",
            "WHERE (is_deleted IS NULL OR is_deleted = 0)",
            "AND (is_visible IS NULL OR is_visible = 1)",
            "AND species IS NOT NULL",
            "AND TRIM(species) != ''",
            BROWSE_FILTERS,
            "GROUP BY TRIM(species)",
            "ORDER BY count DESC, label ASC",
            "</script>"
    })
    List<BrowseFacetItemResponse> selectSpeciesFacets(BrowseSampleQuery query);

    @Select({
            "<script>",
            "SELECT TRIM(sample_type) AS label, COUNT(*) AS count",
            "FROM oscar_sample",
            "WHERE (is_deleted IS NULL OR is_deleted = 0)",
            "AND (is_visible IS NULL OR is_visible = 1)",
            "AND sample_type IS NOT NULL",
            "AND TRIM(sample_type) != ''",
            BROWSE_FILTERS,
            "GROUP BY TRIM(sample_type)",
            "ORDER BY count DESC, label ASC",
            "</script>"
    })
    List<BrowseFacetItemResponse> selectSampleTypeFacets(BrowseSampleQuery query);

    @Select({
            "<script>",
            "SELECT TRIM(tissue) AS label, COUNT(*) AS count",
            "FROM oscar_sample",
            "WHERE (is_deleted IS NULL OR is_deleted = 0)",
            "AND (is_visible IS NULL OR is_visible = 1)",
            "AND tissue IS NOT NULL",
            "AND TRIM(tissue) != ''",
            BROWSE_FILTERS,
            "GROUP BY TRIM(tissue)",
            "ORDER BY count DESC, label ASC",
            "</script>"
    })
    List<BrowseFacetItemResponse> selectTissueFacets(BrowseSampleQuery query);
}
