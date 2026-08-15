package com.oscar.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitWebConfig implements WebMvcConfigurer {

    @Value("${oscar.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${oscar.rate-limit.max-requests-per-window:120}")
    private int maxRequestsPerWindow;

    @Value("${oscar.rate-limit.abuse-threshold:5}")
    private int abuseThreshold;

    @Value("${oscar.rate-limit.abuse-window-seconds:300}")
    private int abuseWindowSeconds;

    @Value("${oscar.rate-limit.ban-duration-seconds:1800}")
    private int banDurationSeconds;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(
                        windowSeconds,
                        maxRequestsPerWindow,
                        abuseThreshold,
                        abuseWindowSeconds,
                        banDurationSeconds
                ))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health/**",
                        "/api/ping/**",
                        "/api/feature-detail/enhancer-regions",
                        "/api/search-result/*/regulatory/bedtools/intersect"
                )
                .order(1);
    }
}
