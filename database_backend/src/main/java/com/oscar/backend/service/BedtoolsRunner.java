package com.oscar.backend.service;

import com.oscar.backend.config.BedtoolsProperties;
import com.oscar.backend.entity.BedtoolsQueryRegion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class BedtoolsRunner {

    private final BedtoolsProperties bedtoolsProperties;

    public BedtoolsRunner(BedtoolsProperties bedtoolsProperties) {
        this.bedtoolsProperties = bedtoolsProperties;
    }

    public List<String> intersect(
            Path queryBed,
            Path trackBed,
            Path genomeFile,
            BedtoolsQueryRegion queryRegion
    ) {
        Path bedGz = resolveBedGz(trackBed);
        Path tbiFile = tabixIndexFor(bedGz);
        if (bedGz != null && tbiFile != null) {
            return tabixQuery(bedGz, tbiFile, queryRegion);
        }
        return bedtoolsIntersect(queryBed, trackBed, genomeFile);
    }

    public List<String> intersectFiles(Path queryBed, Path trackBed) {
        return bedtoolsIntersect(queryBed, trackBed, null);
    }

    public List<String> intersectRegions(Path queryBed, Path trackBed) {
        Path bedGz = resolveBedGz(trackBed);
        Path tbiFile = tabixIndexFor(bedGz);
        if (bedGz == null || tbiFile == null) {
            return bedtoolsIntersect(queryBed, trackBed, null);
        }

        List<String> candidateLines = tabixQueryRegions(bedGz, queryBed);
        if (candidateLines.isEmpty()) return List.of();

        Path candidateBed = null;
        try {
            candidateBed = Files.createTempFile(queryBed.getParent(), "oscar-tabix-candidates-", ".bed");
            Files.write(
                    candidateBed,
                    new LinkedHashSet<>(candidateLines),
                    StandardCharsets.UTF_8
            );
            return bedtoolsIntersect(queryBed, candidateBed, null);
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to prepare batched tabix results: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } finally {
            if (candidateBed != null) {
                try {
                    Files.deleteIfExists(candidateBed);
                } catch (IOException ignored) {
                    // Best-effort cleanup of a request-scoped temporary file.
                }
            }
        }
    }

    /**
     * Resolve the BGZF-compressed (.bed.gz) sibling of a track path.
     * If the given path already ends with .bed.gz, return it directly.
     * If it ends with .bed, look for a .bed.gz sibling on disk.
     */
    private Path resolveBedGz(Path trackBed) {
        String name = trackBed.getFileName().toString();
        if (name.endsWith(".bed.gz")) {
            return Files.exists(trackBed) ? trackBed : null;
        }
        if (name.endsWith(".bed")) {
            Path bedGz = trackBed.resolveSibling(name + ".gz");
            return Files.exists(bedGz) ? bedGz : null;
        }
        return null;
    }

    private Path tabixIndexFor(Path bedGz) {
        if (bedGz == null) {
            return null;
        }
        String name = bedGz.getFileName().toString();
        if (!name.endsWith(".bed.gz")) {
            return null;
        }
        Path tbi = bedGz.resolveSibling(name + ".tbi");
        return Files.exists(tbi) ? tbi : null;
    }

    private List<String> tabixQuery(Path trackBed, Path tbiFile, BedtoolsQueryRegion region) {
        String regionStr = region.getChrom() + ":" + region.getStart() + "-" + region.getEnd();
        ProcessBuilder builder = new ProcessBuilder(
                "tabix",
                trackBed.toString(),
                regionStr
        );

        try {
            Process process = builder.start();
            CompletableFuture<List<String>> stdout = CompletableFuture.supplyAsync(() -> readLines(process.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
            boolean finished = process.waitFor(
                    bedtoolsProperties.getBedtools().getTimeoutSeconds(),
                    TimeUnit.SECONDS
            );
            if (!finished) {
                process.destroyForcibly();
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "tabix query timed out after "
                                + bedtoolsProperties.getBedtools().getTimeoutSeconds()
                                + " seconds",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            List<String> lines = stdout.join();
            String errorText = stderr.join();
            if (process.exitValue() != 0) {
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "tabix query failed: " + summarize(errorText),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            return toBedtoolsFormat(lines, region);
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to start tabix query: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "tabix query was interrupted",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private List<String> tabixQueryRegions(Path trackBed, Path queryBed) {
        ProcessBuilder builder = new ProcessBuilder(
                "tabix",
                "-R",
                queryBed.toString(),
                trackBed.toString()
        );

        try {
            Process process = builder.start();
            CompletableFuture<List<String>> stdout = CompletableFuture.supplyAsync(() -> readLines(process.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
            boolean finished = process.waitFor(
                    bedtoolsProperties.getBedtools().getTimeoutSeconds(),
                    TimeUnit.SECONDS
            );
            if (!finished) {
                process.destroyForcibly();
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "batched tabix query timed out after "
                                + bedtoolsProperties.getBedtools().getTimeoutSeconds()
                                + " seconds",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            List<String> lines = stdout.join();
            String errorText = stderr.join();
            if (process.exitValue() != 0) {
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "batched tabix query failed: " + summarize(errorText),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            return lines;
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to start batched tabix query: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "batched tabix query was interrupted",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private List<String> toBedtoolsFormat(List<String> tabixLines, BedtoolsQueryRegion region) {
        List<String> result = new ArrayList<>();
        for (String line : tabixLines) {
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split("\t", -1);
            if (cols.length < 3) {
                continue;
            }
            long featureStart;
            long featureEnd;
            try {
                featureStart = Long.parseLong(cols[1]);
                featureEnd = Long.parseLong(cols[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            long overlapStart = Math.max(region.getStart(), featureStart);
            long overlapEnd = Math.min(region.getEnd(), featureEnd);
            long overlapBp = overlapEnd - overlapStart;
            if (overlapBp <= 0) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(region.getChrom()).append('\t');
            sb.append(region.getStart()).append('\t');
            sb.append(region.getEnd()).append('\t');
            sb.append(region.getRaw()).append('\t');
            for (int i = 0; i < cols.length; i++) {
                sb.append(cols[i]).append('\t');
            }
            sb.append(overlapBp);
            result.add(sb.toString());
        }
        return result;
    }

    private List<String> bedtoolsIntersect(Path queryBed, Path trackBed, Path genomeFile) {
        List<String> command = new ArrayList<>();
        command.add(bedtoolsProperties.getBedtools().getBinaryPath());
        command.add("intersect");
        command.add("-a");
        command.add(queryBed.toString());
        command.add("-b");
        command.add(trackBed.toString());
        command.add("-wo");

        ProcessBuilder builder = new ProcessBuilder(command);

        try {
            Process process = builder.start();
            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
            boolean finished = process.waitFor(
                    bedtoolsProperties.getBedtools().getTimeoutSeconds(),
                    TimeUnit.SECONDS
            );
            if (!finished) {
                process.destroyForcibly();
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "bedtools intersect timed out after "
                                + bedtoolsProperties.getBedtools().getTimeoutSeconds()
                                + " seconds",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            String outputText = stdout.join();
            String errorText = stderr.join();
            if (process.exitValue() != 0) {
                throw new BedtoolsQueryException(
                        "BEDTOOLS_ERROR",
                        "bedtools intersect failed: " + summarize(errorText),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            if (outputText == null || outputText.isBlank()) {
                return List.of();
            }
            return outputText.lines().toList();
        } catch (IOException exception) {
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "failed to start bedtools intersect: " + exception.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BedtoolsQueryException(
                    "BEDTOOLS_ERROR",
                    "bedtools intersect was interrupted after "
                            + Duration.ofSeconds(bedtoolsProperties.getBedtools().getTimeoutSeconds()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String read(java.io.InputStream inputStream) {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return exception.getMessage();
        }
    }

    private List<String> readLines(java.io.InputStream inputStream) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException exception) {
            return List.of();
        }
        return lines;
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) {
            return "no stderr output";
        }
        String normalized = text.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
