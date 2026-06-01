package com.oscar.backend.service;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class RegulatoryAnnotationCountCache {

    private static final long TTL_MILLIS = 5L * 60L * 1000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 60L * 1000L;

    private final ConcurrentHashMap<CountCacheKey, CountCacheValue> cache = new ConcurrentHashMap<>();
    private volatile long lastCleanupMillis = System.currentTimeMillis();

    public CountResult getOrLoad(CountCacheKey key, LongSupplier loader) {
        long startedAt = System.nanoTime();
        long now = System.currentTimeMillis();
        CountCacheValue cached = cache.get(key);
        if (cached != null && now - cached.cachedAtMillis() <= TTL_MILLIS) {
            return new CountResult(cached.total(), elapsedMillis(startedAt), true);
        }
        if (cached != null) {
            cache.remove(key, cached);
        }

        long total = loader.getAsLong();
        cache.put(key, new CountCacheValue(total, now));
        cleanupExpired(now);
        return new CountResult(total, elapsedMillis(startedAt), false);
    }

    public void clear(String annotationType, String datasetId, String dataDomain) {
        if (annotationType == null) {
            return;
        }

        Iterator<Map.Entry<CountCacheKey, CountCacheValue>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            CountCacheKey key = iterator.next().getKey();
            if (!annotationType.equals(key.annotationType())) {
                continue;
            }
            if (datasetId != null && !datasetId.equals(key.datasetId())) {
                continue;
            }
            if (dataDomain != null && !dataDomain.equals(key.dataDomain())) {
                continue;
            }
            iterator.remove();
        }
    }

    public void clearAll() {
        cache.clear();
    }

    private void cleanupExpired(long now) {
        if (now - lastCleanupMillis < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanupMillis = now;
        cache.entrySet().removeIf(entry -> now - entry.getValue().cachedAtMillis() > TTL_MILLIS);
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    public record CountCacheKey(
            String annotationType,
            String datasetId,
            String dataDomain,
            String targetGene,
            String peak,
            String contextCellType,
            String contextCluster,
            String regionType,
            Double maxFdr,
            Double minLog2fc
    ) {
    }

    public record CountCacheValue(long total, long cachedAtMillis) {
    }

    public record CountResult(long total, long countMillis, boolean cacheHit) {
    }
}
