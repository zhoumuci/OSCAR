package com.oscar.backend.controller;

import com.oscar.backend.entity.VisitorPoint;
import com.oscar.backend.entity.VisitorStats;
import com.oscar.backend.service.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    /**
     * Frontend reports one visit per page load.  Counting happens here
     * instead of an interceptor so in-app navigation (which fires many
     * API requests) does not inflate the counter.
     */
    @PostMapping("/visit")
    public void recordVisit(HttpServletRequest request) {
        visitorService.recordVisit(extractClientIp(request));
        // Resolve geolocation asynchronously for new IPs.
        visitorService.resolveUnresolvedAsync();
    }

    @GetMapping("/map-points")
    public List<VisitorPoint> getMapPoints() {
        return visitorService.getVisitorPoints();
    }

    @GetMapping("/stats")
    public VisitorStats getStats() {
        return visitorService.getStats();
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma < 0 ? ip.trim() : ip.substring(0, comma).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
