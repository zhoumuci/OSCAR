package com.oscar.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache("sampleOverview", 1_500, Duration.ofHours(2)),
                cache("cellTypeComposition", 1_000, Duration.ofHours(2)),
                cache("qcViolin", 120, Duration.ofMinutes(45)),
                cache("umapData", 40, Duration.ofMinutes(30)),
                cache("contextOptions", 1_000, Duration.ofHours(2)),
                cache("featureOccurrence", 250, Duration.ofHours(1)),
                cache("geneExpression", 250, Duration.ofHours(1))
        ));
        return manager;
    }

    private CaffeineCache cache(String name, long maximumSize, Duration idleTtl) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(maximumSize)
                        .expireAfterAccess(idleTtl)
                        .build()
        );
    }
}
