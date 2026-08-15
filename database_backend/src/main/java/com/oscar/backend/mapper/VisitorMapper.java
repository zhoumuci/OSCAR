package com.oscar.backend.mapper;

import com.oscar.backend.entity.VisitorLog;
import com.oscar.backend.entity.VisitorPoint;
import com.oscar.backend.entity.VisitorStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VisitorMapper {

    @Insert("""
            INSERT INTO visitor_log (ip_address, visit_count, first_visit, last_visit)
            VALUES (#{ipAddress}, 1, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                visit_count = visit_count + 1,
                last_visit = NOW()
            """)
    int upsertVisit(@Param("ipAddress") String ipAddress);

    @Update("""
            UPDATE visitor_log
            SET country = #{country},
                city = #{city},
                lat = #{lat},
                lon = #{lon}
            WHERE ip_address = #{ipAddress}
            """)
    int updateGeo(
            @Param("ipAddress") String ipAddress,
            @Param("country") String country,
            @Param("city") String city,
            @Param("lat") Double lat,
            @Param("lon") Double lon
    );

    @Select("""
            SELECT ip_address AS ipAddress,
                   country, city, lat, lon,
                   visit_count AS visitCount,
                   first_visit AS firstVisit,
                   last_visit AS lastVisit
            FROM visitor_log
            WHERE lat IS NOT NULL AND lon IS NOT NULL
            """)
    List<VisitorLog> selectResolved();

    @Select("""
            SELECT ip_address AS ipAddress
            FROM visitor_log
            WHERE lat IS NULL OR lon IS NULL
            ORDER BY last_visit DESC
            LIMIT #{limit}
            """)
    List<String> selectUnresolvedIps(@Param("limit") int limit);

    @Select("""
            SELECT COALESCE(city, country, ip_address) AS name,
                   lat, lon,
                   visit_count AS value
            FROM visitor_log
            WHERE lat IS NOT NULL AND lon IS NOT NULL
            ORDER BY visit_count DESC, last_visit DESC
            LIMIT #{limit}
            """)
    List<VisitorPoint> selectVisitorPoints(@Param("limit") int limit);

    @Select("""
            SELECT COALESCE(SUM(visit_count), 0) AS totalVisitors,
                   COUNT(DISTINCT country) AS countryCount,
                   COALESCE(SUM(CASE WHEN last_visit >= NOW() - INTERVAL 24 HOUR THEN 1 ELSE 0 END), 0) AS activeToday
            FROM visitor_log
            WHERE lat IS NOT NULL AND lon IS NOT NULL
            """)
    VisitorStats selectStats();
}
