package com.oscar.backend.service;

import com.oscar.backend.entity.VisitorPoint;
import com.oscar.backend.entity.VisitorStats;
import com.oscar.backend.mapper.VisitorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorService.class);
    private static final int MAX_GEO_POINTS = 200;
    private static final int RESOLVE_BATCH_SIZE = 10;

    private final VisitorMapper visitorMapper;
    private final VisitorGeoResolver geoResolver;

    public VisitorService(VisitorMapper visitorMapper, VisitorGeoResolver geoResolver) {
        this.visitorMapper = visitorMapper;
        this.geoResolver = geoResolver;
    }

    /**
     * Record a visit from the given IP.  Called by the interceptor on
     * every API request.
     */
    public void recordVisit(String ip) {
        if (ip == null || ip.isBlank()) return;
        visitorMapper.upsertVisit(ip.trim());
    }

    /**
     * Resolve a batch of unresolved IPs synchronously so the frontend
     * map has data to show immediately.
     */
    private void resolveBatchSync() {
        try {
            List<String> unresolvedIps = visitorMapper.selectUnresolvedIps(RESOLVE_BATCH_SIZE);
            for (String ip : unresolvedIps) {
                VisitorGeoResolver.GeoResult geo = geoResolver.resolve(ip.trim());
                if (geo == null) continue;
                visitorMapper.updateGeo(ip.trim(), geo.country(), geo.city(), geo.lat(), geo.lon());
            }
        } catch (Exception e) {
            LOGGER.debug("Sync GeoIP resolution batch failed: {}", e.getMessage());
        }
    }

    /**
     * Background resolution — fired by the interceptor so that
     * subsequent requests eventually fill in all IPs.
     */
    @Async
    public void resolveUnresolvedAsync() {
        try {
            List<String> unresolvedIps = visitorMapper.selectUnresolvedIps(RESOLVE_BATCH_SIZE);
            for (String ip : unresolvedIps) {
                VisitorGeoResolver.GeoResult geo = geoResolver.resolve(ip.trim());
                if (geo == null) continue;
                visitorMapper.updateGeo(ip.trim(), geo.country(), geo.city(), geo.lat(), geo.lon());
            }
        } catch (Exception e) {
            LOGGER.debug("Async GeoIP resolution batch failed: {}", e.getMessage());
        }
    }

    /**
     * Return visitor points for the frontend map.
     * Resolves pending IPs synchronously first so the caller always
     * gets the latest data.
     */
    public List<VisitorPoint> getVisitorPoints() {
        resolveBatchSync();
        return visitorMapper.selectVisitorPoints(MAX_GEO_POINTS);
    }

    public VisitorStats getStats() {
        VisitorStats stats = visitorMapper.selectStats();
        return stats != null ? stats : new VisitorStats(0, 0, 0);
    }
}
