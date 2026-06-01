package com.oscar.backend.mapper;

import com.oscar.backend.entity.ReferenceTrack;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReferenceTrackMapper {

    @Insert("""
            INSERT INTO oscar_reference_track (
                genome_build,
                category,
                source_type,
                label,
                file_path,
                file_format,
                coordinate_system,
                coordinate_mode,
                row_count,
                file_size_bytes,
                md5,
                status,
                description,
                manifest_path,
                display_order
            ) VALUES (
                #{genomeBuild},
                #{category},
                #{sourceType},
                #{label},
                #{filePath},
                #{fileFormat},
                #{coordinateSystem},
                #{coordinateMode},
                #{rowCount},
                #{fileSizeBytes},
                #{md5},
                #{status},
                #{description},
                #{manifestPath},
                #{displayOrder}
            )
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                label = #{label},
                file_path = #{filePath},
                file_format = #{fileFormat},
                coordinate_system = #{coordinateSystem},
                coordinate_mode = #{coordinateMode},
                row_count = #{rowCount},
                file_size_bytes = #{fileSizeBytes},
                md5 = #{md5},
                status = #{status},
                description = #{description},
                manifest_path = #{manifestPath},
                display_order = #{displayOrder},
                updated_at = NOW()
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int upsert(ReferenceTrack track);

    @Select("""
            SELECT *
            FROM oscar_reference_track
            WHERE genome_build = #{genomeBuild}
              AND category = #{category}
            ORDER BY display_order ASC, source_type ASC
            """)
    List<ReferenceTrack> findByGenomeBuildAndCategory(
            @Param("genomeBuild") String genomeBuild,
            @Param("category") String category
    );

    @Select("""
            SELECT *
            FROM oscar_reference_track
            WHERE genome_build = #{genomeBuild}
              AND category = #{category}
              AND source_type = #{sourceType}
            LIMIT 1
            """)
    ReferenceTrack findByGenomeBuildCategoryAndSourceType(
            @Param("genomeBuild") String genomeBuild,
            @Param("category") String category,
            @Param("sourceType") String sourceType
    );
}
