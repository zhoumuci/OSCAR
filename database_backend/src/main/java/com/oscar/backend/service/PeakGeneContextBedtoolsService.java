package com.oscar.backend.service;

import com.oscar.backend.config.BedtoolsProperties;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PeakGeneContextBedtoolsService {

    private static final int QUERY_COLUMN_COUNT = 4;
    private static final int FEATURE_COLUMN_COUNT = 4;
    private static final int OUTPUT_COLUMN_COUNT = QUERY_COLUMN_COUNT + FEATURE_COLUMN_COUNT + 1;

    private final BedtoolsRunner bedtoolsRunner;
    private final BedtoolsProperties bedtoolsProperties;

    public PeakGeneContextBedtoolsService(
            BedtoolsRunner bedtoolsRunner,
            BedtoolsProperties bedtoolsProperties
    ) {
        this.bedtoolsRunner = bedtoolsRunner;
        this.bedtoolsProperties = bedtoolsProperties;
    }

    public List<Map<String, Object>> intersect(
            List<Map<String, Object>> peaks,
            List<Map<String, Object>> candidates,
        int minOverlapBp
    ) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Path queryBed = null;
        Path candidateBed = null;
        try {
            Path tmpRoot = Paths.get(bedtoolsProperties.getBedtools().getTmpRoot()).toAbsolutePath().normalize();
            Files.createDirectories(tmpRoot);
            queryBed = Files.createTempFile(tmpRoot, "oscar-p2g-query-", ".bed");
            candidateBed = Files.createTempFile(tmpRoot, "oscar-p2g-candidates-", ".bed");
            writeQueryBed(queryBed, peaks);
            writeCandidateBed(candidateBed, candidates);

            Set<Integer> matchedIndexes = parseMatchedIndexes(
                    bedtoolsRunner.intersectFiles(queryBed, candidateBed),
                    candidates.size(),
                    minOverlapBp
            );
            List<Map<String, Object>> matched = new ArrayList<>(matchedIndexes.size());
            for (Integer index : matchedIndexes) {
                matched.add(candidates.get(index));
            }
            return matched;
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to prepare Peak-to-Gene BED files: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } finally {
            deleteTemporaryFile(queryBed);
            deleteTemporaryFile(candidateBed);
        }
    }

    private void writeQueryBed(Path output, List<Map<String, Object>> peaks) throws IOException {
        try (BufferedWriter writer = newWriter(output)) {
            for (int index = 0; index < peaks.size(); index++) {
                Map<String, Object> peak = peaks.get(index);
                writeBedRow(
                        writer,
                        requiredString(peak.get("chrom"), "query chromosome"),
                        requiredLong(peak.get("start"), "query start"),
                        requiredLong(peak.get("end"), "query end"),
                        "q_" + index
                );
            }
        }
    }

    private void writeCandidateBed(Path output, List<Map<String, Object>> candidates) throws IOException {
        try (BufferedWriter writer = newWriter(output)) {
            for (int index = 0; index < candidates.size(); index++) {
                Map<String, Object> candidate = candidates.get(index);
                writeBedRow(
                        writer,
                        requiredString(candidate.get("chromosome"), "candidate chromosome"),
                        requiredLong(candidate.get("peak_start"), "candidate start"),
                        requiredLong(candidate.get("peak_end"), "candidate end"),
                        "f_" + index
                );
            }
        }
    }

    private BufferedWriter newWriter(Path output) throws IOException {
        return Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private void writeBedRow(
            BufferedWriter writer,
            String chromosome,
            long start,
            long end,
            String identifier
    ) throws IOException {
        writer.write(chromosome);
        writer.write('\t');
        writer.write(Long.toString(start));
        writer.write('\t');
        writer.write(Long.toString(end));
        writer.write('\t');
        writer.write(identifier);
        writer.write('\n');
    }

    private Set<Integer> parseMatchedIndexes(
            List<String> lines,
            int candidateCount,
            int minOverlapBp
    ) {
        Set<Integer> indexes = new LinkedHashSet<>();
        for (String line : lines) {
            String[] columns = line.split("\t", -1);
            if (columns.length != OUTPUT_COLUMN_COUNT) {
                throw invalidOutput("expected " + OUTPUT_COLUMN_COUNT + " columns but received " + columns.length);
            }
            String featureId = columns[QUERY_COLUMN_COUNT + 3];
            if (!featureId.startsWith("f_")) {
                throw invalidOutput("invalid feature identifier: " + featureId);
            }
            int index;
            long overlapBp;
            try {
                index = Integer.parseInt(featureId.substring(2));
                overlapBp = Long.parseLong(columns[OUTPUT_COLUMN_COUNT - 1]);
            } catch (NumberFormatException exception) {
                throw invalidOutput("non-numeric feature identifier or overlap length");
            }
            if (index < 0 || index >= candidateCount) {
                throw invalidOutput("feature identifier is outside the candidate set: " + featureId);
            }
            if (overlapBp >= minOverlapBp) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private BedtoolsQueryException invalidOutput(String detail) {
        return new BedtoolsQueryException(
                "BEDTOOLS_ERROR",
                "invalid Peak-to-Gene bedtools output: " + detail,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private static String requiredString(Object value, String field) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException(field + " is missing.");
        }
        return value.toString();
    }

    private static long requiredLong(Object value, String field) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(field + " is not numeric.");
    }

    private static void deleteTemporaryFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete temporary BED file: " + path, exception);
        }
    }
}
