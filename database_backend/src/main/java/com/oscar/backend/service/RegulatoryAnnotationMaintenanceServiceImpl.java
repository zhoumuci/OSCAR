package com.oscar.backend.service;

import com.oscar.backend.entity.GeneMarkerSummaryRefreshResponse;
import com.oscar.backend.entity.LinkedRegionRefreshResponse;
import com.oscar.backend.mapper.RegulatoryAnnotationMaintenanceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RegulatoryAnnotationMaintenanceServiceImpl implements RegulatoryAnnotationMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(RegulatoryAnnotationMaintenanceServiceImpl.class);

    private static final String LINKED_REGION_DOMAIN = "integration";
    private static final String SUCCESS_MESSAGE = "Linked region materialized table refreshed successfully.";
    /**
     * Maximum time an admin refresh request will queue behind another
     * in-flight refresh of the same scope before giving up.
     */
    private static final long LOCK_WAIT_TIMEOUT_MINUTES = 180;
    private static final List<EnrichmentReference> ENRICHMENT_REFERENCES = List.of(
            new EnrichmentReference("integration_expression", "integration", List.of("gene_expression", "gene_exp", ""))
    );

    /**
     * Application-level lock to serialize refreshes of the same scope and
     * avoid InnoDB lock-wait timeouts.  Without this, two overlapping
     * DELETE+INSERT…SELECT transactions compete for row locks on the
     * materialized table and the looser hits the 50 s MySQL timeout.
     */
    private final ConcurrentHashMap<String, ReentrantLock> refreshLocks = new ConcurrentHashMap<>();

    private final RegulatoryAnnotationMaintenanceMapper regulatoryAnnotationMaintenanceMapper;
    private final RegulatoryAnnotationCountCache countCache;
    private final TransactionTemplate transactionTemplate;

    public RegulatoryAnnotationMaintenanceServiceImpl(
            RegulatoryAnnotationMaintenanceMapper regulatoryAnnotationMaintenanceMapper,
            RegulatoryAnnotationCountCache countCache,
            TransactionTemplate transactionTemplate
    ) {
        this.regulatoryAnnotationMaintenanceMapper = regulatoryAnnotationMaintenanceMapper;
        this.countCache = countCache;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Block until the lock for {@code lockKey} is available, then return it.
     * If a refresh of the same scope is already running the caller queues
     * behind it instead of racing at the MySQL level.
     */
    private ReentrantLock acquireRefreshLock(String lockKey) {
        ReentrantLock lock = refreshLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            if (lock.isLocked()) {
                log.info("Refresh scope [{}] is already running — queuing behind it (timeout {} min)...",
                        lockKey, LOCK_WAIT_TIMEOUT_MINUTES);
            }
            acquired = lock.tryLock(LOCK_WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Refresh interrupted while waiting for lock: " + lockKey
            );
        }
        if (!acquired) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Timed out waiting for a running refresh of " + lockKey
                    + " to complete. Please try again later."
            );
        }
        return lock;
    }

    private String linkedRegionLockKey(String datasetId, String domain) {
        return "linked_region:" + domain + ":" + (datasetId == null ? "*" : datasetId);
    }

    private String geneSummaryLockKey(String datasetId) {
        return "gene_summary:" + (datasetId == null ? "*" : datasetId);
    }

    @Override
    public LinkedRegionRefreshResponse refreshMarkerLinkedRegions(String datasetId, String domain) {
        String normalizedDatasetId = trimToNull(datasetId);
        String normalizedDomain = normalizeLinkedRegionDomain(domain);
        String lockKey = linkedRegionLockKey(normalizedDatasetId, normalizedDomain);

        ReentrantLock lock = acquireRefreshLock(lockKey);
        try {
            long start = System.currentTimeMillis();
            long totalDeleted = 0;
            long totalInserted = 0;

            // Get all dataset_ids that have marker genes for this domain
            List<String> allIds = regulatoryAnnotationMaintenanceMapper
                    .getDistinctDatasetIdsForDomain(normalizedDomain);

            // Filter to requested dataset if specified
            List<String> candidateIds = normalizedDatasetId != null
                    ? allIds.stream().filter(normalizedDatasetId::equals).toList()
                    : allIds;

            if (candidateIds.isEmpty() && normalizedDatasetId != null) {
                candidateIds = List.of(normalizedDatasetId);
            }

            // Skip datasets that already have linked_region data
            java.util.Set<String> completedIds = new java.util.HashSet<>(
                    regulatoryAnnotationMaintenanceMapper.getCompletedLinkedRegionDatasetIds(normalizedDomain));
            List<String> targetIds = new java.util.ArrayList<>();
            int skipped = 0;
            for (String id : candidateIds) {
                if (completedIds.contains(id)) {
                    skipped++;
                } else {
                    targetIds.add(id);
                }
            }

            if (skipped > 0) {
                log.info("Skipping {} dataset(s) already refreshed", skipped);
            }
            if (targetIds.isEmpty()) {
                log.info("All {} dataset(s) already refreshed — nothing to do", candidateIds.size());
                LinkedRegionRefreshResponse response = new LinkedRegionRefreshResponse();
                response.setDatasetId(normalizedDatasetId);
                response.setDomain(normalizedDomain);
                response.setDeletedRows(0L);
                response.setInsertedRows(0L);
                response.setMessage("All datasets already refreshed. Skipped " + skipped + ".");
                long totalRows = regulatoryAnnotationMaintenanceMapper.countLinkedRegions(
                        normalizedDatasetId, normalizedDomain);
                response.setTotalRowsAfterRefresh(totalRows);
                response.setElapsedMillis(System.currentTimeMillis() - start);
                return response;
            }

            log.info("Refreshing linked_region for domain={} across {} dataset(s) ({} already done)",
                    normalizedDomain, targetIds.size(), skipped);

            for (int i = 0; i < targetIds.size(); i++) {
                String dsId = targetIds.get(i);
                final long[] batchDeleted = {0};
                final long[] batchInserted = {0};

                transactionTemplate.executeWithoutResult(status -> {
                    batchDeleted[0] = regulatoryAnnotationMaintenanceMapper
                            .deleteMarkerLinkedRegions(dsId, normalizedDomain);
                    batchInserted[0] = regulatoryAnnotationMaintenanceMapper
                            .insertMarkerLinkedRegions(dsId, normalizedDomain);
                });

                totalDeleted += batchDeleted[0];
                totalInserted += batchInserted[0];

                if ((i + 1) % 10 == 0 || i == targetIds.size() - 1) {
                    log.info("  [{}/{}] dataset_id={}  deleted={}  inserted={}",
                            i + 1, targetIds.size(), dsId, batchDeleted[0], batchInserted[0]);
                }
            }

            countCache.clear("linked_region", normalizedDatasetId, normalizedDomain);

            LinkedRegionRefreshResponse response = new LinkedRegionRefreshResponse();
            response.setDatasetId(normalizedDatasetId);
            response.setDomain(normalizedDomain);
            response.setDeletedRows(totalDeleted);
            response.setInsertedRows(totalInserted);
            response.setMessage(SUCCESS_MESSAGE);

            long totalRows = regulatoryAnnotationMaintenanceMapper.countLinkedRegions(
                    normalizedDatasetId, normalizedDomain
            );
            response.setTotalRowsAfterRefresh(totalRows);

            response.setDistinctGenes(
                    regulatoryAnnotationMaintenanceMapper.countDistinctGenesInLinkedRegion(
                            normalizedDatasetId, normalizedDomain
                    )
            );
            response.setDistinctPeaks(
                    regulatoryAnnotationMaintenanceMapper.countDistinctPeaksInLinkedRegion(
                            normalizedDatasetId, normalizedDomain
                    )
            );
            response.setRowsWithMarkerPeak(
                    regulatoryAnnotationMaintenanceMapper.countLinkedRegionWithMarkerPeak(
                            normalizedDatasetId, normalizedDomain
                    )
            );
            response.setRowsWithoutMarkerPeak(totalRows - response.getRowsWithMarkerPeak());

            List<Map<String, Object>> signalRows = regulatoryAnnotationMaintenanceMapper.getSignalTypeCounts(
                    normalizedDatasetId, normalizedDomain
            );
            Map<String, Long> signalCounts = new LinkedHashMap<>();
            for (Map<String, Object> row : signalRows) {
                String st = (String) row.get("signal_type");
                Object cnt = row.get("cnt");
                signalCounts.put(st == null || st.isEmpty() ? "(empty)" : st,
                        cnt instanceof Number ? ((Number) cnt).longValue() : 0L);
            }
            response.setSignalTypeCounts(signalCounts);

            response.setElapsedMillis(System.currentTimeMillis() - start);
            return response;
        } finally {
            lock.unlock();
            refreshLocks.remove(lockKey);
        }
    }

    @Override
    public GeneMarkerSummaryRefreshResponse refreshGeneMarkerSummary(String datasetId) {
        String normalizedDatasetId = trimToNull(datasetId);
        String lockKey = geneSummaryLockKey(normalizedDatasetId);

        ReentrantLock lock = acquireRefreshLock(lockKey);
        try {
            long start = System.currentTimeMillis();
            int totalDeleted = 0, totalInserted = 0, skipped = 0;

            if (normalizedDatasetId != null) {
                // single dataset: direct refresh
                transactionTemplate.executeWithoutResult(status -> {
                    regulatoryAnnotationMaintenanceMapper.deleteGeneMarkerSummary(normalizedDatasetId);
                    regulatoryAnnotationMaintenanceMapper.insertGeneMarkerSummary(normalizedDatasetId);
                });
                totalDeleted = -1; totalInserted = -1; // exact count not tracked per-ds in this path
            } else {
                // all datasets: loop with resume
                List<String> allIds = regulatoryAnnotationMaintenanceMapper.getDistinctDatasetIdsForDomain("integration");
                java.util.Set<String> completed = new java.util.HashSet<>(
                        regulatoryAnnotationMaintenanceMapper.getCompletedGeneSummaryDatasetIds());
                List<String> targetIds = new java.util.ArrayList<>();
                for (String id : allIds) {
                    if (completed.contains(id)) { skipped++; }
                    else { targetIds.add(id); }
                }
                log.info("Gene summary refresh: {} total, {} already done, {} to refresh", allIds.size(), skipped, targetIds.size());

                int[] del = {0}, ins = {0};
                for (String dsId : targetIds) {
                    transactionTemplate.executeWithoutResult(status -> {
                        regulatoryAnnotationMaintenanceMapper.deleteGeneMarkerSummary(dsId);
                        int n = regulatoryAnnotationMaintenanceMapper.insertGeneMarkerSummary(dsId);
                        del[0]++; ins[0] += n;
                    });
                }
                totalDeleted = del[0]; totalInserted = ins[0];
            }

            GeneMarkerSummaryRefreshResponse response = new GeneMarkerSummaryRefreshResponse();
            response.setDatasetId(normalizedDatasetId);
            response.setDeletedRows((long) totalDeleted);
            response.setInsertedRows((long) totalInserted);
            response.setSkippedDatasets(skipped);
            if (normalizedDatasetId == null) {
                response.setTotalDatasets(regulatoryAnnotationMaintenanceMapper.getDistinctDatasetIdsForDomain("integration").size());
            }

            long totalRows = regulatoryAnnotationMaintenanceMapper.countGeneMarkerSummary(normalizedDatasetId);
            response.setTotalRowsAfterRefresh(totalRows);
            response.setDistinctGenes(regulatoryAnnotationMaintenanceMapper.countDistinctGenesInSummary(normalizedDatasetId));

            List<Map<String, Object>> domainRows = regulatoryAnnotationMaintenanceMapper.getSummaryDomainCounts(normalizedDatasetId);
            Map<String, Long> domainCounts = new LinkedHashMap<>();
            for (Map<String, Object> row : domainRows) {
                domainCounts.put(String.valueOf(row.get("domain")),
                        row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L);
            }
            response.setDomainCounts(domainCounts);

            List<Map<String, Object>> signalRows = regulatoryAnnotationMaintenanceMapper.getSummarySignalTypeCounts(normalizedDatasetId);
            Map<String, Long> signalCounts = new LinkedHashMap<>();
            for (Map<String, Object> row : signalRows) {
                String st = (String) row.get("signal_type");
                signalCounts.put(st == null || st.isEmpty() ? "(empty)" : st,
                        row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L);
            }
            response.setSignalTypeCounts(signalCounts);

            if (normalizedDatasetId == null) {
                long statsStart = System.currentTimeMillis();
                long statsRows = refreshGlobalCellTypeEnrichmentStats();
                response.setEnrichmentStatsRows(statsRows);
                response.setEnrichmentStatsElapsedMillis(System.currentTimeMillis() - statsStart);
            }

            response.setElapsedMillis(System.currentTimeMillis() - start);
            return response;
        } finally {
            lock.unlock();
            refreshLocks.remove(lockKey);
        }
    }

    private long refreshGlobalCellTypeEnrichmentStats() {
        regulatoryAnnotationMaintenanceMapper.deleteGlobalEnrichmentUniverses();
        regulatoryAnnotationMaintenanceMapper.deleteGlobalCellTypeEnrichmentSets();
        long rows = 0L;
        for (EnrichmentReference reference : ENRICHMENT_REFERENCES) {
            regulatoryAnnotationMaintenanceMapper.upsertGlobalEnrichmentUniverse(
                    reference.markerReference(), reference.domain(), reference.signalTypes());
            rows += Math.max(0, regulatoryAnnotationMaintenanceMapper.upsertGlobalCellTypeEnrichmentSets(
                    reference.markerReference(), reference.domain(), reference.signalTypes()));
        }
        log.info("Cell enrichment global stats refreshed: {} marker-set rows", rows);
        return rows;
    }

    /**
     * linked_region refresh only supports domain=integration.
     * RNA and ATAC return 400.
     */
    private String normalizeLinkedRegionDomain(String domain) {
        String normalized = trimToNull(domain);
        String value = normalized == null ? LINKED_REGION_DOMAIN : normalized.toLowerCase(Locale.ROOT);
        if (!LINKED_REGION_DOMAIN.equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "linked_region refresh only supports domain=integration. Got: " + value
            );
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private record EnrichmentReference(String markerReference, String domain, List<String> signalTypes) {}
}
