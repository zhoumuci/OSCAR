package com.oscar.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(2, TimeUnit.HOURS));
        // no TTL — cache lives until data refresh evicts, or 2h idle
        mgr.setCacheNames(java.util.List.of(
                "sampleOverview",
                "cellTypeComposition",
                "qcViolin",
                "umapData",
                "contextOptions",
                "featureOccurrence",
                "geneExpression"
        ));
        return mgr;
    }
}
