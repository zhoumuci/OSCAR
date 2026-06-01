package com.oscar.backend.service;

import com.oscar.backend.config.BedtoolsProperties;
import com.oscar.backend.entity.BedtoolsIntersectRequest;
import com.oscar.backend.entity.BedtoolsIntersectResponse;
import com.oscar.backend.entity.BedtoolsIntersectSummary;
import com.oscar.backend.entity.BedtoolsOverlapRecord;
import com.oscar.backend.entity.BedtoolsQueryRegion;
import com.oscar.backend.entity.BedtoolsRawOverlap;
import com.oscar.backend.entity.BedtoolsSourcesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BedtoolsQueryServiceImpl implements BedtoolsQueryService {

    private static final String DEFAULT_DOMAIN = "integration";
    private static final String DEFAULT_GENOME_BUILD = "hg38";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String COORDINATE_SYSTEM = "BED 0-based half-open";
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final List<BedtoolsAnnotationType> SORT_ORDER = List.of(
            BedtoolsAnnotationType.MARKER_PEAK,
            BedtoolsAnnotationType.P2G_LINK,
            BedtoolsAnnotationType.RISK_SNP,
            BedtoolsAnnotationType.COMMON_SNP,
            BedtoolsAnnotationType.GTEX_EQTL,
            BedtoolsAnnotationType.TFBS,
            BedtoolsAnnotationType.ENHANCER,
            BedtoolsAnnotationType.SUPER_ENHANCER,
            BedtoolsAnnotationType.GENE,
            BedtoolsAnnotationType.TRANSCRIPT,
            BedtoolsAnnotationType.TSS_PROMOTER,
            BedtoolsAnnotationType.TF_ANNOTATION
    );

    private final BedtoolsProperties bedtoolsProperties;
    private final BedtoolsRegionParser regionParser;
    private final BedtoolsTrackResolver trackResolver;
    private final BedtoolsRunner bedtoolsRunner;
    private final BedtoolsResultHydrator resultHydrator;

    public BedtoolsQueryServiceImpl(
            BedtoolsProperties bedtoolsProperties,
            BedtoolsRegionParser regionParser,
            BedtoolsTrackResolver trackResolver,
            BedtoolsRunner bedtoolsRunner,
            BedtoolsResultHydrator resultHydrator
    ) {
        this.bedtoolsProperties = bedtoolsProperties;
        this.regionParser = regionParser;
        this.trackResolver = trackResolver;
        this.bedtoolsRunner = bedtoolsRunner;
        this.resultHydrator = resultHydrator;
    }

    @Override
    public BedtoolsSourcesResponse getSources(String datasetId, String domain, String genomeBuild) {
        String normalizedDatasetId = normalizeRequiredSegment(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        String normalizedGenomeBuild = normalizeOptionalSegment(genomeBuild, DEFAULT_GENOME_BUILD, true, "genomeBuild");

        BedtoolsSourcesResponse response = new BedtoolsSourcesResponse();
        response.setDatasetId(normalizedDatasetId);
        response.setDomain(normalizedDomain);
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setCoordinateSystem(COORDINATE_SYSTEM);
        response.setSources(trackResolver.listSources(normalizedDatasetId, normalizedDomain, normalizedGenomeBuild));
        return response;
    }

    @Override
    public BedtoolsIntersectResponse intersect(String datasetId, BedtoolsIntersectRequest request) {
        long startedAt = System.nanoTime();
        String normalizedDatasetId = normalizeRequiredSegment(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(request == null ? null : request.getDomain());
        String normalizedGenomeBuild = normalizeOptionalSegment(
                request == null ? null : request.getGenomeBuild(),
                DEFAULT_GENOME_BUILD,
                true,
                "genomeBuild"
        );
        BedtoolsQueryRegion queryRegion = regionParser.parse(
                request == null ? null : request.getRegion(),
                bedtoolsProperties.getBedtools().getMaxRegionBp()
        );
        List<BedtoolsAnnotationType> annotationTypes = normalizeAnnotationTypes(
                request == null ? null : request.getAnnotationTypes(),
                normalizedGenomeBuild
        );
        int minOverlapBp = normalizeMinOverlap(request == null ? null : request.getMinOverlapBp());
        int page = normalizePage(request == null ? null : request.getPage());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());

        List<BedtoolsTrackResolver.ResolvedTrack> tracks = annotationTypes
                .stream()
                .map(type -> trackResolver.resolveForIntersect(type, normalizedDatasetId, normalizedDomain, normalizedGenomeBuild))
                .toList();

        Path queryBed = null;
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());
        List<BedtoolsRawOverlap> rawOverlaps;
        Path genomeFile = Paths.get(
                bedtoolsProperties.getBedtools().getReferenceRoot(),
                normalizedGenomeBuild,
                "fasta",
                "genome.fa.fai"
        ).toAbsolutePath().normalize();
        try {
            queryBed = writeQueryBed(queryRegion);
            Path finalQueryBed = queryBed;
            rawOverlaps = tracks.parallelStream()
                    .flatMap(track -> {
                        List<String> lines = bedtoolsRunner.intersect(finalQueryBed, track.path(), genomeFile, queryRegion);
                        return parseBedtoolsOutput(track, queryRegion, lines, minOverlapBp, warnings).stream();
                    })
                    .collect(Collectors.toList());
        } finally {
            if (queryBed != null) {
                deleteQuietly(queryBed);
            }
        }

        List<BedtoolsOverlapRecord> hydrated = new ArrayList<>(resultHydrator.hydrate(
                rawOverlaps,
                normalizedDatasetId,
                normalizedDomain,
                normalizedGenomeBuild
        ));
        hydrated.sort(recordComparator());

        long total = hydrated.size();
        int fromIndex = Math.min((page - 1) * pageSize, hydrated.size());
        int toIndex = Math.min(fromIndex + pageSize, hydrated.size());
        List<BedtoolsOverlapRecord> pageRecords = hydrated.subList(fromIndex, toIndex);

        BedtoolsIntersectSummary summary = new BedtoolsIntersectSummary();
        summary.setTotalHits(total);
        summary.setByAnnotationType(countByAnnotationType(annotationTypes, hydrated));
        summary.setElapsedMillis((System.nanoTime() - startedAt) / 1_000_000L);

        BedtoolsIntersectResponse response = new BedtoolsIntersectResponse();
        response.setStatus(STATUS_SUCCESS);
        response.setMessage("OK");
        response.setDatasetId(normalizedDatasetId);
        response.setDomain(normalizedDomain);
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setCoordinateSystem(COORDINATE_SYSTEM);
        response.setQueryRegion(queryRegion);
        response.setSelectedAnnotationTypes(annotationTypes.stream().map(BedtoolsAnnotationType::value).toList());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(total);
        response.setSummary(summary);
        response.setRecords(pageRecords);
        response.setWarnings(warnings);
        return response;
    }

    private Path writeQueryBed(BedtoolsQueryRegion region) {
        Path tmpRoot = Paths.get(bedtoolsProperties.getBedtools().getTmpRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(tmpRoot);
            Path queryBed = Files.createTempFile(tmpRoot, "oscar-bedtools-query-", ".bed");
            try (BufferedWriter writer = Files.newBufferedWriter(
                    queryBed,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                writer.write(String.join(
                        "\t",
                        region.getChrom(),
                        Long.toString(region.getStart()),
                        Long.toString(region.getEnd()),
                        region.getRaw()
                ));
                writer.write('\n');
            }
            return queryBed;
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to create temporary query BED: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private List<BedtoolsRawOverlap> parseBedtoolsOutput(
            BedtoolsTrackResolver.ResolvedTrack track,
            BedtoolsQueryRegion queryRegion,
            List<String> lines,
            int minOverlapBp,
            List<String> warnings
    ) {
        List<BedtoolsRawOverlap> overlaps = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            if (columns.length < 8) {
                warnings.add("Skipped malformed bedtools row for " + track.type().value() + ": expected at least 8 columns.");
                continue;
            }

            int overlapIndex = columns.length - 1;
            long overlapBp;
            long featureStart;
            long featureEnd;
            try {
                overlapBp = Long.parseLong(columns[overlapIndex]);
                featureStart = Long.parseLong(columns[5]);
                featureEnd = Long.parseLong(columns[6]);
            } catch (NumberFormatException exception) {
                warnings.add("Skipped malformed bedtools row for " + track.type().value() + ": invalid numeric field.");
                continue;
            }
            if (overlapBp < minOverlapBp) {
                continue;
            }
            if (featureEnd <= featureStart) {
                warnings.add("Skipped malformed bedtools row for " + track.type().value() + ": invalid feature interval.");
                continue;
            }

            List<String> featureColumns = new ArrayList<>();
            for (int index = 4; index < overlapIndex; index++) {
                featureColumns.add(columns[index]);
            }

            BedtoolsRawOverlap raw = new BedtoolsRawOverlap();
            raw.setAnnotationType(track.type().value());
            raw.setScope(track.scope());
            raw.setFeatureChrom(columns[4]);
            raw.setFeatureStart(featureStart);
            raw.setFeatureEnd(featureEnd);
            String bedName = featureColumns.size() > 3 ? featureColumns.get(3).trim() : "";
            boolean nameIsValid = !bedName.isEmpty() && !".".equals(bedName);
            raw.setFeatureId(nameIsValid
                    ? bedName
                    : track.type().value() + ":" + columns[4] + ":" + featureStart + "-" + featureEnd);
            raw.setOverlapBp(overlapBp);
            raw.setOverlapRatioQuery(ratio(overlapBp, queryRegion.length()));
            raw.setOverlapRatioFeature(ratio(overlapBp, featureEnd - featureStart));
            raw.setFeatureColumns(featureColumns);
            overlaps.add(raw);
        }
        return overlaps;
    }

    private List<BedtoolsAnnotationType> normalizeAnnotationTypes(List<String> values, String genomeBuild) {
        if (values == null || values.isEmpty()) {
            List<BedtoolsAnnotationType> defaults = trackResolver.getAvailableRegulatoryTypes(genomeBuild);
            if (defaults.isEmpty()) {
                throw new BedtoolsQueryException(
                        "NO_AVAILABLE_SOURCES",
                        "No regulatory reference tracks are available for genomeBuild=" + genomeBuild + ". "
                                + "Run POST /api/admin/reference-tracks/refresh first.",
                        HttpStatus.CONFLICT
                );
            }
            return defaults;
        }
        Set<BedtoolsAnnotationType> selected = new LinkedHashSet<>();
        for (String value : values) {
            BedtoolsAnnotationType type = BedtoolsAnnotationType.fromValue(value);
            if (type == null) {
                throw new BedtoolsQueryException(
                        "INVALID_ANNOTATION_TYPES",
                        "annotationTypes must use supported values: " + BedtoolsAnnotationType.allowedValues(),
                        HttpStatus.BAD_REQUEST
                );
            }
            if (type == BedtoolsAnnotationType.TF_ANNOTATION) {
                throw new BedtoolsQueryException(
                        "TRACK_NOT_AVAILABLE",
                        "TF annotation data have not been integrated yet.",
                        HttpStatus.BAD_REQUEST
                );
            }
            selected.add(type);
        }
        if (selected.isEmpty()) {
            throw new BedtoolsQueryException(
                    "INVALID_ANNOTATION_TYPES",
                    "annotationTypes must not be empty",
                    HttpStatus.BAD_REQUEST
            );
        }
        return List.copyOf(selected);
    }

    private Map<String, Long> countByAnnotationType(
            List<BedtoolsAnnotationType> annotationTypes,
            List<BedtoolsOverlapRecord> records
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BedtoolsAnnotationType type : annotationTypes) {
            counts.put(type.value(), 0L);
        }
        for (BedtoolsOverlapRecord record : records) {
            counts.computeIfPresent(record.getAnnotationType(), (key, value) -> value + 1L);
        }
        return counts;
    }

    private Comparator<BedtoolsOverlapRecord> recordComparator() {
        return Comparator
                .comparingInt((BedtoolsOverlapRecord record) -> annotationSortIndex(record.getAnnotationType()))
                .thenComparing((left, right) -> Long.compare(
                        nullToZero(right.getOverlapBp()),
                        nullToZero(left.getOverlapBp())
                ))
                .thenComparing(record -> nullToEmpty(record.getFeatureRegion()))
                .thenComparing(record -> nullToEmpty(record.getFeatureId()));
    }

    private int annotationSortIndex(String annotationType) {
        BedtoolsAnnotationType type = BedtoolsAnnotationType.fromValue(annotationType);
        int index = SORT_ORDER.indexOf(type);
        return index < 0 ? SORT_ORDER.size() : index;
    }

    private Double ratio(long numerator, long denominator) {
        if (denominator <= 0L) {
            return null;
        }
        return numerator / (double) denominator;
    }

    private String normalizeRequiredSegment(String value, String parameterName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BedtoolsQueryException("INVALID_REQUEST", parameterName + " is required", HttpStatus.BAD_REQUEST);
        }
        validatePathSegment(normalized, parameterName);
        return normalized;
    }

    private String normalizeDomain(String value) {
        String normalized = normalizeOptionalSegment(value, DEFAULT_DOMAIN, true, "domain");
        if (!List.of("integration", "rna", "atac").contains(normalized)) {
            throw new BedtoolsQueryException(
                    "INVALID_REQUEST",
                    "domain must be integration, RNA, or ATAC",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private String normalizeOptionalSegment(
            String value,
            String defaultValue,
            boolean lowerCase,
            String parameterName
    ) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            normalized = defaultValue;
        }
        if (lowerCase) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        validatePathSegment(normalized, parameterName);
        return normalized;
    }

    private void validatePathSegment(String value, String parameterName) {
        if (".".equals(value) || "..".equals(value) || !SAFE_PATH_SEGMENT.matcher(value).matches()) {
            throw new BedtoolsQueryException(
                    "INVALID_REQUEST",
                    parameterName + " may contain only letters, numbers, dot, underscore, and dash",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private int normalizeMinOverlap(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int normalizePageSize(Integer value) {
        if (value == null || value < 1) {
            return 10;
        }
        return Math.min(value, 100);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary query BED cleanup is best effort.
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
