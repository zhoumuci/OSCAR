package com.oscar.backend.service;

import com.oscar.backend.entity.LinkedRegionRefreshResponse;
import com.oscar.backend.mapper.RegulatoryAnnotationMaintenanceMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegulatoryAnnotationMaintenanceServiceImpl implements RegulatoryAnnotationMaintenanceService {

    private static final String DEFAULT_DOMAIN = "integration";
    private static final String SUCCESS_MESSAGE = "Linked region materialized table refreshed successfully.";

    private final RegulatoryAnnotationMaintenanceMapper regulatoryAnnotationMaintenanceMapper;
    private final RegulatoryAnnotationCountCache countCache;

    public RegulatoryAnnotationMaintenanceServiceImpl(
            RegulatoryAnnotationMaintenanceMapper regulatoryAnnotationMaintenanceMapper,
            RegulatoryAnnotationCountCache countCache
    ) {
        this.regulatoryAnnotationMaintenanceMapper = regulatoryAnnotationMaintenanceMapper;
        this.countCache = countCache;
    }

    @Override
    @Transactional
    public LinkedRegionRefreshResponse refreshMarkerLinkedRegions(String datasetId, String domain) {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);

        int deletedRows = regulatoryAnnotationMaintenanceMapper.deleteMarkerLinkedRegions(
                normalizedDatasetId,
                normalizedDomain
        );
        int insertedRows = regulatoryAnnotationMaintenanceMapper.insertMarkerLinkedRegions(
                normalizedDatasetId,
                normalizedDomain
        );
        countCache.clear("linked_region", normalizedDatasetId, normalizedDomain);

        return new LinkedRegionRefreshResponse(
                normalizedDatasetId,
                normalizedDomain,
                (long) deletedRows,
                (long) insertedRows,
                SUCCESS_MESSAGE
        );
    }

    private String normalizeRequired(String value, String parameterName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " is required");
        }
        return normalized;
    }

    private String normalizeDomain(String domain) {
        String normalized = trimToNull(domain);
        return normalized == null ? DEFAULT_DOMAIN : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }
}
