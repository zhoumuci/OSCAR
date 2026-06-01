package com.oscar.backend.service;

import com.oscar.backend.entity.BedtoolsQueryRegion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BedtoolsRegionParser {

    private static final Pattern REGION_PATTERN = Pattern.compile("^([^:\\s]+):(\\d[\\d,]*)-(\\d[\\d,]*)$");

    public BedtoolsQueryRegion parse(String rawRegion, long maxRegionBp) {
        String normalized = trimToNull(rawRegion);
        if (normalized == null) {
            throw invalidRegion("region is required");
        }

        Matcher matcher = REGION_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw invalidRegion("region must use chr:start-end BED-style coordinates");
        }

        try {
            long start = Long.parseLong(matcher.group(2).replace(",", ""));
            long end = Long.parseLong(matcher.group(3).replace(",", ""));
            if (start < 0L || end <= start) {
                throw invalidRegion("region must satisfy start >= 0 and end > start");
            }
            long length = end - start;
            if (length > maxRegionBp) {
                throw new BedtoolsQueryException(
                        "REGION_TOO_LARGE",
                        "region length " + length + " bp exceeds maximum " + maxRegionBp + " bp",
                        HttpStatus.BAD_REQUEST
                );
            }
            return new BedtoolsQueryRegion(normalized, matcher.group(1), start, end);
        } catch (NumberFormatException exception) {
            throw invalidRegion("region coordinates must be valid integers");
        }
    }

    private BedtoolsQueryException invalidRegion(String message) {
        return new BedtoolsQueryException("INVALID_REGION", message, HttpStatus.BAD_REQUEST);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }
}
