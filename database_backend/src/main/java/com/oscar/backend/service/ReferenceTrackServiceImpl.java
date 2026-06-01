package com.oscar.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.backend.config.BedtoolsProperties;
import com.oscar.backend.entity.ReferenceTrack;
import com.oscar.backend.entity.ReferenceTrackDto;
import com.oscar.backend.entity.ReferenceTrackRefreshResponse;
import com.oscar.backend.entity.ReferenceTrackStatusResponse;
import com.oscar.backend.mapper.ReferenceTrackMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ReferenceTrackServiceImpl implements ReferenceTrackService {

    private static final String DEFAULT_GENOME_BUILD = "hg38";
    private static final String DEFAULT_CATEGORY = "regulatory";
    private static final String MANIFEST_JSON = "manifest.json";
    private static final String FILE_FORMAT_BED = "BED";
    private static final String DEFAULT_COORDINATE_SYSTEM = "BED 0-based half-open";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_MISSING = "MISSING";
    private static final String STATUS_ERROR = "ERROR";
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");

    private static final Map<String, String> DEFAULT_LABELS = Map.of(
            "common_snp", "Common SNP",
            "risk_snp", "Risk SNP",
            "gtex_eqtl", "GTEx eQTL",
            "tfbs", "TFBS",
            "enhancer", "Enhancer",
            "super_enhancer", "Super Enhancer",
            "methylation", "Methylation",
            "crispr", "CRISPR"
    );

    private static final Map<String, Integer> DISPLAY_ORDERS = Map.of(
            "risk_snp", 10,
            "common_snp", 20,
            "gtex_eqtl", 30,
            "tfbs", 40,
            "enhancer", 50,
            "super_enhancer", 60,
            "methylation", 70,
            "crispr", 80
    );

    private final ReferenceTrackMapper referenceTrackMapper;
    private final BedtoolsProperties bedtoolsProperties;
    private final ObjectMapper objectMapper;

    public ReferenceTrackServiceImpl(
            ReferenceTrackMapper referenceTrackMapper,
            BedtoolsProperties bedtoolsProperties,
            ObjectMapper objectMapper
    ) {
        this.referenceTrackMapper = referenceTrackMapper;
        this.bedtoolsProperties = bedtoolsProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ReferenceTrackRefreshResponse refreshReferenceTracks(String genomeBuild, String category) {
        String normalizedGenomeBuild = normalizeOptionalSegment(
                genomeBuild,
                DEFAULT_GENOME_BUILD,
                true,
                "genomeBuild"
        );
        String normalizedCategory = normalizeOptionalSegment(category, DEFAULT_CATEGORY, true, "category");
        Path manifestPath = resolveManifestPath(normalizedGenomeBuild, normalizedCategory);

        if (!Files.exists(manifestPath)) {
            throw new ReferenceTrackException(
                    HttpStatus.NOT_FOUND,
                    "MANIFEST_NOT_FOUND",
                    "Reference manifest not found: " + pathString(manifestPath)
            );
        }
        if (!Files.isRegularFile(manifestPath)) {
            throw new ReferenceTrackException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MANIFEST",
                    "Reference manifest is not a regular file: " + pathString(manifestPath)
            );
        }

        ParseResult parseResult = parseManifest(manifestPath);
        List<String> warnings = new ArrayList<>(parseResult.warnings());
        List<ReferenceTrackDto> dtos = new ArrayList<>();
        int ready = 0;
        int missing = 0;
        int error = 0;

        for (TrackCandidate candidate : parseResult.tracks()) {
            ReferenceTrack track = buildTrack(
                    candidate,
                    normalizedGenomeBuild,
                    normalizedCategory,
                    manifestPath,
                    warnings
            );
            referenceTrackMapper.upsert(track);

            ReferenceTrackDto dto = ReferenceTrackDto.fromTrack(track);
            dto.setWarningCount(candidate.warningCount());
            dtos.add(dto);

            if (STATUS_READY.equals(track.getStatus())) {
                ready++;
            } else if (STATUS_MISSING.equals(track.getStatus())) {
                missing++;
            } else if (STATUS_ERROR.equals(track.getStatus())) {
                error++;
            }
        }

        ReferenceTrackRefreshResponse response = new ReferenceTrackRefreshResponse();
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setCategory(normalizedCategory);
        response.setManifestPath(pathString(manifestPath));
        response.setRefreshed(dtos.size());
        response.setReady(ready);
        response.setMissing(missing);
        response.setError(error);
        response.setTracks(dtos);
        response.setWarnings(warnings);
        return response;
    }

    @Override
    public ReferenceTrackStatusResponse getReferenceTrackStatus(String genomeBuild, String category) {
        String normalizedGenomeBuild = normalizeOptionalSegment(
                genomeBuild,
                DEFAULT_GENOME_BUILD,
                true,
                "genomeBuild"
        );
        String normalizedCategory = normalizeOptionalSegment(category, DEFAULT_CATEGORY, true, "category");

        List<ReferenceTrackDto> tracks = referenceTrackMapper
                .findByGenomeBuildAndCategory(normalizedGenomeBuild, normalizedCategory)
                .stream()
                .map(ReferenceTrackDto::fromTrack)
                .toList();

        ReferenceTrackStatusResponse response = new ReferenceTrackStatusResponse();
        response.setGenomeBuild(normalizedGenomeBuild);
        response.setCategory(normalizedCategory);
        response.setTracks(tracks);
        return response;
    }

    private ReferenceTrack buildTrack(
            TrackCandidate candidate,
            String genomeBuild,
            String category,
            Path manifestPath,
            List<String> warnings
    ) {
        Path filePath = resolveTrackPath(candidate, manifestPath, warnings);
        FileState fileState = inspectFile(filePath);
        String sourceType = candidate.sourceType();
        String status;
        String description;

        if (!fileState.exists()) {
            status = STATUS_MISSING;
            description = "Track file is missing: " + pathString(filePath);
        } else if (!fileState.regularFile()) {
            status = STATUS_ERROR;
            description = "Track path is not a regular file: " + pathString(filePath);
        } else if (fileState.errorMessage() != null) {
            status = STATUS_ERROR;
            description = "Failed to inspect track file: " + fileState.errorMessage();
        } else if (fileState.fileSizeBytes() == null || fileState.fileSizeBytes() <= 0L) {
            status = STATUS_ERROR;
            description = "Track file exists but is empty: " + pathString(filePath);
        } else {
            status = STATUS_READY;
            description = trimToNull(candidate.description());
        }

        if (candidate.warningCount() != null && candidate.warningCount() > 0) {
            description = appendDescription(
                    description,
                    "Manifest warningCount=" + candidate.warningCount()
            );
        }

        ReferenceTrack track = new ReferenceTrack();
        track.setGenomeBuild(genomeBuild);
        track.setCategory(category);
        track.setSourceType(sourceType);
        track.setLabel(defaultIfBlank(candidate.label(), defaultLabel(sourceType)));
        track.setFilePath(pathString(filePath));
        track.setFileFormat(FILE_FORMAT_BED);
        track.setCoordinateSystem(defaultIfBlank(candidate.coordinateSystem(), DEFAULT_COORDINATE_SYSTEM));
        track.setCoordinateMode(trimToNull(candidate.coordinateMode()));
        track.setRowCount(candidate.rowCount());
        track.setFileSizeBytes(fileState.fileSizeBytes());
        track.setMd5(trimToNull(candidate.md5()));
        track.setStatus(status);
        track.setDescription(description);
        track.setManifestPath(pathString(manifestPath));
        track.setDisplayOrder(DISPLAY_ORDERS.getOrDefault(sourceType, 100));
        return track;
    }

    private ParseResult parseManifest(Path manifestPath) {
        JsonNode root;
        try {
            root = objectMapper.readTree(manifestPath.toFile());
        } catch (IOException exception) {
            throw new ReferenceTrackException(
                    HttpStatus.BAD_REQUEST,
                    "MANIFEST_PARSE_FAILED",
                    "Failed to parse reference manifest: " + pathString(manifestPath) + ": " + summarize(exception),
                    exception
            );
        }

        List<TrackCandidate> tracks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        JsonNode tracksNode = root.path("tracks");

        if (tracksNode.isArray()) {
            parseArrayTracks(tracksNode, tracks, warnings);
        } else if (tracksNode.isObject()) {
            parseObjectTracks(tracksNode, tracks, warnings);
        } else if (root.isArray()) {
            parseArrayTracks(root, tracks, warnings);
        } else if (root.isObject()) {
            parseObjectTracks(root, tracks, warnings);
        } else {
            warnings.add("Manifest root is not an object or array; no reference tracks were refreshed.");
        }

        if (tracks.isEmpty()) {
            warnings.add("No reference tracks found in manifest.");
        }
        return new ParseResult(tracks, warnings);
    }

    private void parseArrayTracks(
            JsonNode tracksNode,
            List<TrackCandidate> tracks,
            List<String> warnings
    ) {
        int index = 0;
        for (JsonNode item : tracksNode) {
            if (!item.isObject()) {
                warnings.add("Skipped manifest track at index " + index + ": entry is not an object.");
            } else {
                TrackCandidate candidate = parseTrackCandidate(item, null, "tracks[" + index + "]", warnings);
                if (candidate != null) {
                    tracks.add(candidate);
                }
            }
            index++;
        }
    }

    private void parseObjectTracks(
            JsonNode objectNode,
            List<TrackCandidate> tracks,
            List<String> warnings
    ) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (!value.isObject()) {
                continue;
            }
            if (!isTrackLike(entry.getKey(), value)) {
                continue;
            }
            TrackCandidate candidate = parseTrackCandidate(value, entry.getKey(), entry.getKey(), warnings);
            if (candidate != null) {
                tracks.add(candidate);
            }
        }
    }

    private TrackCandidate parseTrackCandidate(
            JsonNode node,
            String fallbackSourceType,
            String location,
            List<String> warnings
    ) {
        String rawSourceType = firstText(node, "sourceType", "source_type", "trackType", "track_type");
        if (isBlank(rawSourceType) && isKnownSourceType(fallbackSourceType)) {
            rawSourceType = fallbackSourceType;
        }
        if (isBlank(rawSourceType)) {
            warnings.add("Skipped manifest track " + location + ": missing sourceType/source_type.");
            return null;
        }

        String sourceType = rawSourceType.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_PATH_SEGMENT.matcher(sourceType).matches()) {
            warnings.add("Skipped manifest track " + location + ": invalid sourceType " + rawSourceType + ".");
            return null;
        }

        return new TrackCandidate(
                sourceType,
                firstText(node, "label"),
                firstText(node, "outputPath", "output_path", "filePath", "file_path", "path"),
                firstLong(node, "rowCount", "row_count", "recordCount", "record_count"),
                firstText(node, "coordinateSystem", "coordinate_system"),
                firstText(node, "coordinateMode", "coordinate_mode"),
                firstText(node, "md5", "md5sum"),
                firstText(node, "description"),
                firstInteger(node, "warningCount", "warning_count")
        );
    }

    private boolean isTrackLike(String key, JsonNode node) {
        return isKnownSourceType(key)
                || hasAny(node, "sourceType", "source_type", "trackType", "track_type")
                || hasAny(node, "outputPath", "output_path", "filePath", "file_path", "path");
    }

    private Path resolveTrackPath(TrackCandidate candidate, Path manifestPath, List<String> warnings) {
        String outputPath = trimToNull(candidate.outputPath());
        if (outputPath == null) {
            warnings.add(
                    "Manifest track "
                            + candidate.sourceType()
                            + " has no outputPath/filePath; using "
                            + candidate.sourceType()
                            + ".bed in the manifest directory."
            );
            return manifestPath.getParent().resolve(candidate.sourceType() + ".bed").normalize();
        }

        // Normalize backslashes from Windows-generated manifests to forward slashes
        // so that Paths.get() correctly splits path components on Linux.
        String normalizedOutput = outputPath.replace('\\', '/');
        Path path = Paths.get(normalizedOutput);
        if (!path.isAbsolute()) {
            Path fileName = path.getFileName();
            if (fileName != null) {
                path = manifestPath.getParent().resolve(fileName);
            } else {
                path = manifestPath.getParent().resolve(path);
            }
        }
        return path.normalize();
    }

    private FileState inspectFile(Path filePath) {
        if (!Files.exists(filePath)) {
            return new FileState(false, false, null, null);
        }
        boolean regularFile = Files.isRegularFile(filePath);
        if (!regularFile) {
            return new FileState(true, false, null, null);
        }
        try {
            return new FileState(true, true, Files.size(filePath), null);
        } catch (IOException exception) {
            return new FileState(true, true, null, summarize(exception));
        }
    }

    private Path resolveManifestPath(String genomeBuild, String category) {
        Path root = Paths.get(bedtoolsProperties.getBedtools().getReferenceRoot())
                .toAbsolutePath()
                .normalize();
        Path manifestPath = root.resolve(genomeBuild).resolve(category).resolve(MANIFEST_JSON).normalize();
        if (!manifestPath.startsWith(root)) {
            throw new ReferenceTrackException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REFERENCE_PATH",
                    "Invalid reference manifest path segment"
            );
        }
        return manifestPath;
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
            throw new ReferenceTrackException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PARAMETER",
                    parameterName + " may contain only letters, numbers, dot, underscore, and dash"
            );
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                String text = null;
                if (value.isTextual()) {
                    text = value.textValue();
                } else if (value.isNumber() || value.isBoolean()) {
                    text = value.asText();
                }
                if (!isBlank(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private Long firstLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isIntegralNumber()) {
                return value.asLong();
            }
            if (value.isTextual()) {
                try {
                    return Long.parseLong(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Integer firstInteger(JsonNode node, String... fieldNames) {
        Long value = firstLong(node, fieldNames);
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }

    private boolean hasAny(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnownSourceType(String value) {
        return value != null && DEFAULT_LABELS.containsKey(value.trim().toLowerCase(Locale.ROOT));
    }

    private String defaultLabel(String sourceType) {
        String configured = DEFAULT_LABELS.get(sourceType);
        if (configured != null) {
            return configured;
        }

        String[] parts = sourceType.split("[_-]+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
        }
        return words.isEmpty() ? sourceType : String.join(" ", words);
    }

    private String appendDescription(String description, String addition) {
        if (isBlank(description)) {
            return addition;
        }
        return description + " " + addition;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
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

    private String pathString(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private String summarize(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }

    private record ParseResult(List<TrackCandidate> tracks, List<String> warnings) {
    }

    private record TrackCandidate(
            String sourceType,
            String label,
            String outputPath,
            Long rowCount,
            String coordinateSystem,
            String coordinateMode,
            String md5,
            String description,
            Integer warningCount
    ) {
    }

    private record FileState(
            boolean exists,
            boolean regularFile,
            Long fileSizeBytes,
            String errorMessage
    ) {
    }
}
