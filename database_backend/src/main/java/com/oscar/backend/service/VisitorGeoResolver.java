package com.oscar.backend.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves IP addresses to geographic coordinates using a local
 * <a href="https://dev.maxmind.com/geoip/geolite2-free-geolocation-data">MaxMind GeoLite2 City</a>
 * database file.
 *
 * <p>No network access is required after the database file is downloaded and
 * placed at the configured path ({@code oscar.geoip.database-path}).</p>
 *
 * <p>Results are cached indefinitely.  Private / reserved IPs are skipped.</p>
 */
@Component
public class VisitorGeoResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorGeoResolver.class);

    private final DatabaseReader reader;
    private final Map<String, GeoResult> cache = new ConcurrentHashMap<>();

    public VisitorGeoResolver(@Value("${oscar.geoip.database-path}") String databasePath) throws IOException {
        File dbFile = new File(databasePath);
        if (!dbFile.exists()) {
            LOGGER.warn(
                    "GeoIP database not found at {}. "
                            + "Download GeoLite2-City.mmdb from https://dev.maxmind.com/geoip/geolite2-free-geolocation-data "
                            + "and place it at the configured path. "
                            + "Visitor map will show no points until the file is present.",
                    databasePath
            );
            reader = null;
            return;
        }
        reader = new DatabaseReader.Builder(dbFile).build();
        LOGGER.info("GeoIP database loaded from {}", databasePath);
    }

    /**
     * Resolve an IP address, returning {@code null} on any failure.
     * Internal/private IPs naturally fail resolution (they're absent from
     * the GeoIP database) so we let MaxMind handle that instead of
     * pre-filtering.
     */
    public GeoResult resolve(String ip) {
        if (reader == null || ip == null || ip.isBlank()) return null;

        return cache.computeIfAbsent(ip, this::lookup);
    }

    private GeoResult lookup(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            CityResponse city = reader.city(addr);

            String country = city.getCountry().getName();
            String cityName = city.getCity().getName();
            double lat = city.getLocation().getLatitude();
            double lon = city.getLocation().getLongitude();

            if (country == null || (lat == 0.0 && lon == 0.0)) {
                return null;
            }

            return new GeoResult(country, cityName != null ? cityName : "", lat, lon);
        } catch (IOException | GeoIp2Exception e) {
            LOGGER.debug("GeoIP lookup failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }

    public record GeoResult(String country, String city, double lat, double lon) {}
}
