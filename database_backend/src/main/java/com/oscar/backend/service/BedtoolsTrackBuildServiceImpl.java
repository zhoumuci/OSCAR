package com.oscar.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.backend.config.BedtoolsProperties;
import com.oscar.backend.entity.BedtoolsMarkerPeakTrackRow;
import com.oscar.backend.entity.BedtoolsP2gTrackRow;
import com.oscar.backend.entity.BedtoolsTrackBuildAllResponse;
import com.oscar.backend.entity.BedtoolsTrackBuildResult;
import com.oscar.backend.entity.BedtoolsTrackBundle;
import com.oscar.backend.entity.BedtoolsTrackItem;
import com.oscar.backend.entity.BedtoolsTrackItemStatusResponse;
import com.oscar.backend.entity.BedtoolsTrackSourceSummary;
import com.oscar.backend.entity.BedtoolsTrackStatusResponse;
import com.oscar.backend.mapper.BedtoolsTrackMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BedtoolsTrackBuildServiceImpl implements BedtoolsTrackBuildService {

    private static final String DEFAULT_DOMAIN = "integration";
    private static final String DEFAULT_GENOME_BUILD = "hg38";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_MISSING = "MISSING";
    private static final String TRACK_MARKER_PEAK = "marker_peak";
    private static final String TRACK_P2G_LINK = "p2g_link";
    private static final String MARKER_PEAK_BED = "marker_peaks.bed";
    private static final String P2G_LINK_BED = "p2g_links.bed";
    private static final String MANIFEST_JSON = "manifest.json";
    private static final String ACTION_BUILT = "built";
    private static final String ACTION_SKIPPED = "skipped";
    private static final String ACTION_FAILED = "failed";
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern REGION_PATTERN = Pattern.compile("^([^:\\s]+):(\\d[\\d,]*)-(\\d[\\d,]*)$");

    private final BedtoolsTrackMapper bedtoolsTrackMapper;
    private final BedtoolsProperties bedtoolsProperties;
    private final ObjectMapper objectMapper;

    public BedtoolsTrackBuildServiceImpl(
            BedtoolsTrackMapper bedtoolsTrackMapper,
            BedtoolsProperties bedtoolsProperties,
            ObjectMapper objectMapper
    ) {
        this.bedtoolsTrackMapper = bedtoolsTrackMapper;
        this.bedtoolsProperties = bedtoolsProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public BedtoolsTrackStatusResponse getTrackStatus(String datasetId, String domain, String genomeBuild) {
        String normalizedDatasetId = normalizeRequiredSegment(datasetId, "datasetId");
        String normalizedDomain = normalizeOptionalSegment(domain, DEFAULT_DOMAIN, true, "domain");
        String normalizedGenomeBuild = normalizeOptionalSegment(genomeBuild, DEFAULT_GENOME_BUILD, true, "genomeBuild");
        return getTrackStatusInternal(normalizedDatasetId, normalizedDomain, normalizedGenomeBuild, null);
    }

    @Override
    public BedtoolsTrackStatusResponse buildSampleTracks(
            String datasetId,
            String domain,
            String genomeBuild,
            boolean force
    ) {
        String normalizedDatasetId = normalizeRequiredSegment(datasetId, "datasetId");
        String normalizedDomain = normalizeOptionalSegment(domain, DEFAULT_DOMAIN, true, "domain");
        String normalizedGenomeBuild = normalizeOptionalSegment(genomeBuild, DEFAULT_GENOME_BUILD, true, "genomeBuild");
        TrackPaths paths = resolveTrackPaths(normalizedDatasetId, normalizedDomain);

        BedtoolsTrackBundle existingBundle = bedtoolsTrackMapper.selectBundle(
                normalizedDatasetId,
                normalizedDomain,
                normalizedGenomeBuild
        );
        if (!force && readyFilesExist(existingBundle, paths)) {
            return getTrackStatusInternal(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGenomeBuild,
                    ACTION_SKIPPED
            );
        }

        try {
            Files.createDirectories(paths.sampleDir());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create sample track directory", exception);
        }

        try (BuildLock ignored = acquireBuildLock(paths.lockFile())) {
            existingBundle = bedtoolsTrackMapper.selectBundle(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGenomeBuild
            );
            if (!force && readyFilesExist(existingBundle, paths)) {
                return getTrackStatusInternal(
                        normalizedDatasetId,
                        normalizedDomain,
                        normalizedGenomeBuild,
                        ACTION_SKIPPED
                );
            }

            return buildSampleTracksLocked(normalizedDatasetId, normalizedDomain, normalizedGenomeBuild, paths);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build sample BED tracks", exception);
        }
    }

    @Override
    public BedtoolsTrackBuildAllResponse buildAllSampleTracks(String domain, String genomeBuild, boolean force) {
        String normalizedDomain = normalizeOptionalSegment(domain, DEFAULT_DOMAIN, true, "domain");
        String normalizedGenomeBuild = normalizeOptionalSegment(genomeBuild, DEFAULT_GENOME_BUILD, true, "genomeBuild");
        List<String> datasetIds = bedtoolsTrackMapper.selectDatasetIdsForTrackBuild(normalizedDomain);

        BedtoolsTrackBuildAllResponse response = new BedtoolsTrackBuildAllResponse();
        response.setDomain(normalizedDomain);
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setForce(force);
        response.setTotal(datasetIds.size());

        List<BedtoolsTrackBuildResult> results = new ArrayList<>();
        int built = 0;
        int skipped = 0;
        int failed = 0;

        for (String datasetId : datasetIds) {
            try {
                BedtoolsTrackStatusResponse status = buildSampleTracks(
                        datasetId,
                        normalizedDomain,
                        normalizedGenomeBuild,
                        force
                );
                String action = ACTION_SKIPPED.equals(status.getMessage()) ? ACTION_SKIPPED : ACTION_BUILT;
                if (STATUS_FAILED.equals(status.getStatus())) {
                    action = ACTION_FAILED;
                }

                if (ACTION_SKIPPED.equals(action)) {
                    skipped++;
                } else if (ACTION_FAILED.equals(action)) {
                    failed++;
                } else {
                    built++;
                }

                results.add(new BedtoolsTrackBuildResult(
                        datasetId,
                        status.getStatus(),
                        action,
                        status.getMessage(),
                        status.getErrorMessage()
                ));
            } catch (Exception exception) {
                failed++;
                results.add(new BedtoolsTrackBuildResult(
                        datasetId,
                        STATUS_FAILED,
                        ACTION_FAILED,
                        ACTION_FAILED,
                        errorMessage(exception)
                ));
            }
        }

        response.setBuilt(built);
        response.setSkipped(skipped);
        response.setFailed(failed);
        response.setResults(results);
        return response;
    }

    private BedtoolsTrackStatusResponse buildSampleTracksLocked(
            String datasetId,
            String domain,
            String genomeBuild,
            TrackPaths paths
    ) {
        Long bundleId = null;
        String sourceFingerprint = null;

        try {
            cleanupTemporaryFiles(paths);

            BedtoolsTrackBundle bundle = new BedtoolsTrackBundle();
            bundle.setDatasetId(datasetId);
            bundle.setDataDomain(domain);
            bundle.setGenomeBuild(genomeBuild);
            bundle.setSourceFingerprint(sourceFingerprint);
            bundle.setReferenceVersion(genomeBuild);
            bedtoolsTrackMapper.upsertBundleBuilding(bundle);
            bundleId = bundle.getId();
            if (bundleId == null) {
                BedtoolsTrackBundle selected = bedtoolsTrackMapper.selectBundle(datasetId, domain, genomeBuild);
                bundleId = selected == null ? null : selected.getId();
            }
            if (bundleId == null) {
                throw new IllegalStateException("Failed to resolve BEDTools track bundle id");
            }

            bedtoolsTrackMapper.upsertTrackItemBuilding(bundleId, TRACK_MARKER_PEAK);
            bedtoolsTrackMapper.upsertTrackItemBuilding(bundleId, TRACK_P2G_LINK);

            sourceFingerprint = buildSourceFingerprint(datasetId, domain);
            ExportResult markerPeakResult = writeMarkerPeakBed(datasetId, domain, paths.markerPeakTmp());
            ExportResult p2gResult = writeP2gBed(datasetId, domain, paths.p2gTmp());
            LocalDateTime generatedAt = LocalDateTime.now();
            writeManifest(datasetId, domain, genomeBuild, generatedAt, markerPeakResult, p2gResult, paths.manifestTmp());
            publishGeneratedFiles(paths);

            bedtoolsTrackMapper.updateTrackItemReady(
                    bundleId,
                    TRACK_MARKER_PEAK,
                    pathString(paths.markerPeakBed()),
                    markerPeakResult.recordCount(),
                    markerPeakResult.skippedCount()
            );
            bedtoolsTrackMapper.updateTrackItemReady(
                    bundleId,
                    TRACK_P2G_LINK,
                    pathString(paths.p2gBed()),
                    p2gResult.recordCount(),
                    p2gResult.skippedCount()
            );
            bedtoolsTrackMapper.updateBundleReady(
                    bundleId,
                    pathString(paths.manifest()),
                    sourceFingerprint,
                    genomeBuild,
                    generatedAt
            );

            return getTrackStatusInternal(datasetId, domain, genomeBuild, ACTION_BUILT);
        } catch (Exception exception) {
            String message = errorMessage(exception);
            cleanupTemporaryFilesQuietly(paths);
            if (bundleId != null) {
                bedtoolsTrackMapper.updateBuildingTrackItemsFailed(bundleId, message);
                bedtoolsTrackMapper.updateBundleFailed(bundleId, message);
            }
            return getTrackStatusInternal(datasetId, domain, genomeBuild, ACTION_FAILED);
        }
    }

    private BedtoolsTrackStatusResponse getTrackStatusInternal(
            String datasetId,
            String domain,
            String genomeBuild,
            String message
    ) {
        TrackPaths paths = resolveTrackPaths(datasetId, domain);
        BedtoolsTrackBundle bundle = bedtoolsTrackMapper.selectBundle(datasetId, domain, genomeBuild);
        List<BedtoolsTrackItem> items = bundle == null ? List.of() : bedtoolsTrackMapper.selectItems(bundle.getId());
        Map<String, BedtoolsTrackItem> itemsByType = items.stream()
                .collect(Collectors.toMap(BedtoolsTrackItem::getTrackType, item -> item, (left, right) -> left));

        BedtoolsTrackStatusResponse response = new BedtoolsTrackStatusResponse();
        response.setDatasetId(datasetId);
        response.setDomain(domain);
        response.setGenomeBuild(genomeBuild);
        response.setManifestPath(bundle == null || isBlank(bundle.getManifestPath())
                ? pathString(paths.manifest())
                : bundle.getManifestPath());
        response.setSourceFingerprint(bundle == null ? null : bundle.getSourceFingerprint());
        response.setReferenceVersion(bundle == null || isBlank(bundle.getReferenceVersion())
                ? genomeBuild
                : bundle.getReferenceVersion());
        response.setStartedAt(bundle == null ? null : bundle.getStartedAt());
        response.setFinishedAt(bundle == null ? null : bundle.getFinishedAt());
        response.setGeneratedAt(bundle == null ? null : bundle.getGeneratedAt());
        response.setErrorMessage(bundle == null ? null : bundle.getErrorMessage());
        response.setMessage(message);

        List<BedtoolsTrackItemStatusResponse> itemStatuses = List.of(
                itemStatus(TRACK_MARKER_PEAK, itemsByType.get(TRACK_MARKER_PEAK), paths.markerPeakBed()),
                itemStatus(TRACK_P2G_LINK, itemsByType.get(TRACK_P2G_LINK), paths.p2gBed())
        );
        response.setItems(itemStatuses);

        String status = bundle == null ? STATUS_MISSING : bundle.getStatus();
        if (STATUS_READY.equals(status)
                && (!Files.exists(paths.markerPeakBed()) || !Files.exists(paths.p2gBed()) || !Files.exists(paths.manifest()))) {
            status = STATUS_MISSING;
        }
        response.setStatus(status);
        return response;
    }

    private BedtoolsTrackItemStatusResponse itemStatus(String trackType, BedtoolsTrackItem item, Path expectedPath) {
        String status = item == null ? STATUS_MISSING : item.getStatus();
        String trackPath = item == null || isBlank(item.getTrackPath()) ? pathString(expectedPath) : item.getTrackPath();
        String errorMessage = item == null ? null : item.getErrorMessage();
        if (STATUS_READY.equals(status) && !Files.exists(expectedPath)) {
            status = STATUS_MISSING;
            errorMessage = "Track file is missing.";
        }
        return new BedtoolsTrackItemStatusResponse(
                trackType,
                status,
                trackPath,
                item == null || item.getRecordCount() == null ? 0L : item.getRecordCount(),
                item == null || item.getSkippedCount() == null ? 0L : item.getSkippedCount(),
                errorMessage
        );
    }

    private boolean readyFilesExist(BedtoolsTrackBundle bundle, TrackPaths paths) {
        return bundle != null
                && STATUS_READY.equals(bundle.getStatus())
                && Files.exists(paths.markerPeakBed())
                && Files.exists(paths.p2gBed())
                && Files.exists(paths.manifest());
    }

    private ExportResult writeMarkerPeakBed(String datasetId, String domain, Path outputPath) throws IOException {
        List<BedtoolsMarkerPeakTrackRow> rows = bedtoolsTrackMapper.selectMarkerPeakTrackRows(datasetId, domain);
        long recordCount = 0L;
        long skippedCount = 0L;

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            for (BedtoolsMarkerPeakTrackRow row : rows) {
                Coordinates coordinates = resolveCoordinates(
                        row.getChromosome(),
                        row.getPeakStart(),
                        row.getPeakEnd(),
                        row.getPeakName()
                );
                if (coordinates == null || row.getId() == null) {
                    skippedCount++;
                    continue;
                }

                // Source peak coordinates are treated as BED-like 0-based half-open.
                // Export preserves start/end exactly as provided by source files; no start - 1 conversion.
                writer.write(String.join(
                        "\t",
                        coordinates.chromosome(),
                        Long.toString(coordinates.start()),
                        Long.toString(coordinates.end()),
                        "MP:" + row.getId(),
                        bedField(row.getGroupName()),
                        bedField(row.getCellType()),
                        bedField(row.getLog2fc()),
                        bedField(row.getFdr()),
                        bedField(row.getPeakName())
                ));
                writer.write('\n');
                recordCount++;
            }
        }

        return new ExportResult(recordCount, skippedCount);
    }

    private ExportResult writeP2gBed(String datasetId, String domain, Path outputPath) throws IOException {
        List<BedtoolsP2gTrackRow> rows = bedtoolsTrackMapper.selectP2gTrackRows(datasetId, domain);
        long recordCount = 0L;
        long skippedCount = 0L;

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            for (BedtoolsP2gTrackRow row : rows) {
                Coordinates coordinates = resolveCoordinates(
                        row.getChromosome(),
                        row.getPeakStart(),
                        row.getPeakEnd(),
                        row.getLinkedPeakRegion()
                );
                if (coordinates == null || row.getId() == null) {
                    skippedCount++;
                    continue;
                }

                // P2G exports the linked peak region, not the target gene interval.
                // Source linked peak coordinates are BED-like 0-based half-open and are preserved as-is.
                writer.write(String.join(
                        "\t",
                        coordinates.chromosome(),
                        Long.toString(coordinates.start()),
                        Long.toString(coordinates.end()),
                        "P2G:" + row.getId(),
                        bedField(row.getTargetGene()),
                        bedField(row.getLinkedPeakRegion()),
                        bedField(row.getCellType()),
                        bedField(row.getCluster()),
                        bedField(row.getP2gScore())
                ));
                writer.write('\n');
                recordCount++;
            }
        }

        return new ExportResult(recordCount, skippedCount);
    }

    private void writeManifest(
            String datasetId,
            String domain,
            String genomeBuild,
            LocalDateTime generatedAt,
            ExportResult markerPeakResult,
            ExportResult p2gResult,
            Path outputPath
    ) throws IOException {
        String generatedAtText = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(generatedAt);
        Path referenceGenomeRoot = Paths.get(bedtoolsProperties.getBedtools().getReferenceRoot())
                .resolve(genomeBuild)
                .normalize();

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("datasetId", datasetId);
        manifest.put("domain", domain);
        manifest.put("genomeBuild", genomeBuild);
        manifest.put("coordinateSystem", "BED 0-based half-open");
        manifest.put(
                "sourceCoordinatePolicy",
                "marker_peak and P2G linked peak coordinates are preserved as provided by source files"
        );
        manifest.put("generatedAt", generatedAtText);
        manifest.put("tracks", List.of(
                trackManifest(TRACK_MARKER_PEAK, MARKER_PEAK_BED, "oscar_marker_peak", markerPeakResult),
                trackManifest(TRACK_P2G_LINK, P2G_LINK_BED, "oscar_marker_linked_region", p2gResult)
        ));
        manifest.put("referenceVersion", genomeBuild);
        manifest.put(
                "notes",
                "Sample-specific tracks only. Reference tracks are stored globally under " + referenceGenomeRoot + "."
        );

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), manifest);
    }

    private Map<String, Object> trackManifest(
            String trackType,
            String path,
            String sourceTable,
            ExportResult exportResult
    ) {
        Map<String, Object> track = new LinkedHashMap<>();
        track.put("trackType", trackType);
        track.put("path", path);
        track.put("sourceTable", sourceTable);
        track.put("recordCount", exportResult.recordCount());
        track.put("skippedCount", exportResult.skippedCount());
        return track;
    }

    private String buildSourceFingerprint(String datasetId, String domain) {
        BedtoolsTrackSourceSummary markerPeak = bedtoolsTrackMapper.selectMarkerPeakSourceSummary(datasetId, domain);
        BedtoolsTrackSourceSummary p2g = bedtoolsTrackMapper.selectP2gSourceSummary(datasetId, domain);
        String fingerprint = "marker_peak:"
                + summaryCount(markerPeak)
                + ":"
                + summaryUpdatedAt(markerPeak)
                + "|p2g_link:"
                + summaryCount(p2g)
                + ":"
                + summaryUpdatedAt(p2g);
        return fingerprint.length() <= 256 ? fingerprint : fingerprint.substring(0, 256);
    }

    private long summaryCount(BedtoolsTrackSourceSummary summary) {
        return summary == null || summary.getRecordCount() == null ? 0L : summary.getRecordCount();
    }

    private String summaryUpdatedAt(BedtoolsTrackSourceSummary summary) {
        if (summary == null || summary.getMaxUpdatedAt() == null) {
            return "none";
        }
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(summary.getMaxUpdatedAt());
    }

    private Coordinates resolveCoordinates(String chromosome, Long start, Long end, String regionText) {
        String normalizedChromosome = trimToNull(chromosome);
        if (normalizedChromosome != null && isValidInterval(start, end)) {
            return new Coordinates(normalizedChromosome, start, end);
        }

        String normalizedRegion = trimToNull(regionText);
        if (normalizedRegion == null) {
            return null;
        }

        Matcher matcher = REGION_PATTERN.matcher(normalizedRegion);
        if (!matcher.matches()) {
            return null;
        }

        try {
            long parsedStart = Long.parseLong(matcher.group(2).replace(",", ""));
            long parsedEnd = Long.parseLong(matcher.group(3).replace(",", ""));
            if (!isValidInterval(parsedStart, parsedEnd)) {
                return null;
            }
            return new Coordinates(matcher.group(1), parsedStart, parsedEnd);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isValidInterval(Long start, Long end) {
        return start != null && end != null && start >= 0L && end > start;
    }

    private void publishGeneratedFiles(TrackPaths paths) throws IOException {
        String publishId = UUID.randomUUID().toString();
        List<PublishState> states = new ArrayList<>();

        try {
            publishOne(paths.markerPeakTmp(), paths.markerPeakBed(), publishId, states);
            publishOne(paths.p2gTmp(), paths.p2gBed(), publishId, states);
            publishOne(paths.manifestTmp(), paths.manifest(), publishId, states);

            for (PublishState state : states) {
                if (state.backupMoved()) {
                    Files.deleteIfExists(state.backup());
                }
            }
        } catch (IOException exception) {
            rollbackPublishedFiles(states);
            throw exception;
        }
    }

    private void publishOne(Path tmp, Path target, String publishId, List<PublishState> states) throws IOException {
        Path backup = target.resolveSibling("." + target.getFileName() + "." + publishId + ".bak");
        PublishState state = new PublishState(target, backup, Files.exists(target));
        states.add(state);

        Files.deleteIfExists(backup);
        if (state.hadTarget()) {
            atomicMove(target, backup);
            state.setBackupMoved(true);
        }
        atomicMove(tmp, target);
        state.setTargetPublished(true);
    }

    private void rollbackPublishedFiles(List<PublishState> states) {
        for (int index = states.size() - 1; index >= 0; index--) {
            PublishState state = states.get(index);
            try {
                if (state.targetPublished()) {
                    Files.deleteIfExists(state.target());
                }
                if (state.backupMoved() && Files.exists(state.backup())) {
                    atomicMove(state.backup(), state.target());
                }
            } catch (IOException ignored) {
                // Best effort rollback; DB status records the failure for admin recovery.
            }
        }
    }

    private void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private BuildLock acquireBuildLock(Path lockFile) throws IOException {
        FileChannel channel = FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        );
        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException exception) {
            channel.close();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sample BED track build is already running.");
        }
        if (lock == null) {
            channel.close();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sample BED track build is already running.");
        }
        return new BuildLock(channel, lock);
    }

    private void cleanupTemporaryFiles(TrackPaths paths) throws IOException {
        Files.deleteIfExists(paths.markerPeakTmp());
        Files.deleteIfExists(paths.p2gTmp());
        Files.deleteIfExists(paths.manifestTmp());
    }

    private void cleanupTemporaryFilesQuietly(TrackPaths paths) {
        try {
            cleanupTemporaryFiles(paths);
        } catch (IOException ignored) {
            // Build status retains the original failure; temporary cleanup is best effort.
        }
    }

    private TrackPaths resolveTrackPaths(String datasetId, String domain) {
        Path root = Paths.get(bedtoolsProperties.getBedtools().getSampleTrackRoot())
                .toAbsolutePath()
                .normalize();
        Path sampleDir = root.resolve(datasetId).resolve(domain).normalize();
        if (!sampleDir.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sample track path segment");
        }

        return new TrackPaths(
                sampleDir,
                sampleDir.resolve(".build.lock"),
                sampleDir.resolve(MARKER_PEAK_BED),
                sampleDir.resolve(P2G_LINK_BED),
                sampleDir.resolve(MANIFEST_JSON),
                sampleDir.resolve("marker_peaks.tmp.bed"),
                sampleDir.resolve("p2g_links.tmp.bed"),
                sampleDir.resolve("manifest.tmp.json")
        );
    }

    private String normalizeRequiredSegment(String value, String parameterName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " is required");
        }
        validatePathSegment(normalized, parameterName);
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    parameterName + " may contain only letters, numbers, dot, underscore, and dash"
            );
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String bedField(Object value) {
        if (value == null) {
            return ".";
        }
        String raw = value instanceof Double doubleValue
                ? Double.toString(doubleValue)
                : String.valueOf(value);
        String normalized = raw.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.isEmpty() ? "." : normalized;
    }

    private String pathString(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private String errorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private record ExportResult(long recordCount, long skippedCount) {
    }

    private record Coordinates(String chromosome, long start, long end) {
    }

    private record TrackPaths(
            Path sampleDir,
            Path lockFile,
            Path markerPeakBed,
            Path p2gBed,
            Path manifest,
            Path markerPeakTmp,
            Path p2gTmp,
            Path manifestTmp
    ) {
    }

    private record BuildLock(FileChannel channel, FileLock lock) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            lock.release();
            channel.close();
        }
    }

    private static final class PublishState {
        private final Path target;
        private final Path backup;
        private final boolean hadTarget;
        private boolean backupMoved;
        private boolean targetPublished;

        private PublishState(Path target, Path backup, boolean hadTarget) {
            this.target = target;
            this.backup = backup;
            this.hadTarget = hadTarget;
        }

        private Path target() {
            return target;
        }

        private Path backup() {
            return backup;
        }

        private boolean hadTarget() {
            return hadTarget;
        }

        private boolean backupMoved() {
            return backupMoved;
        }

        private void setBackupMoved(boolean backupMoved) {
            this.backupMoved = backupMoved;
        }

        private boolean targetPublished() {
            return targetPublished;
        }

        private void setTargetPublished(boolean targetPublished) {
            this.targetPublished = targetPublished;
        }
    }
}
