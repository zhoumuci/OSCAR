package com.oscar.backend.service;

import com.oscar.backend.config.BlastConfig;
import com.oscar.backend.entity.SequencePeak2GeneResponse.BlastHitDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class BlastService {

    private static final Logger log = LoggerFactory.getLogger(BlastService.class);
    private static final int MIN_SEQ_LENGTH = 10;

    private final BlastConfig config;

    public BlastService(BlastConfig config) {
        this.config = config;
    }

    /** Clean and validate a DNA sequence. Returns cleaned uppercase string. */
    public String cleanSequence(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Sequence is empty.");
        }
        // strip FASTA headers, whitespace
        String cleaned = raw.lines()
                .filter(line -> !line.startsWith(">"))
                .collect(java.util.stream.Collectors.joining());
        cleaned = cleaned.replaceAll("\\s+", "").toUpperCase();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Sequence is empty after removing FASTA headers.");
        }
        // check length
        if (cleaned.length() < MIN_SEQ_LENGTH) {
            throw new IllegalArgumentException(
                    "Sequence too short: " + cleaned.length() + " bp (minimum " + MIN_SEQ_LENGTH + ").");
        }
        // validate characters
        String invalid = cleaned.replaceAll("[ACGTN]", "");
        if (!invalid.isEmpty()) {
            Set<Character> bad = new LinkedHashSet<>();
            for (char c : invalid.toCharArray()) bad.add(c);
            throw new IllegalArgumentException("Invalid nucleotide characters: " + bad);
        }
        return cleaned;
    }

    /** Run BLAST and return parsed hits. Returns empty list if no hits. */
    public List<BlastHitDto> runBlast(String cleanedSeq, String blastTask, int maxTargetSeqs,
                                       int maxHsps, double evalueCutoff) throws IOException {
        if (!config.isEnabled()) {
            throw new IllegalStateException("BLAST is not enabled on this server.");
        }
        Path dbPrefix = Path.of(config.getDbPath());
        if (!Files.exists(Path.of(config.getDbPath() + ".nhr"))
                && !Files.exists(Path.of(config.getDbPath() + ".00.nhr"))) {
            throw new IllegalStateException("BLAST database not found at: " + config.getDbPath());
        }

        // resolve blast task
        String task = blastTask;
        if ("auto".equalsIgnoreCase(task)) {
            task = cleanedSeq.length() <= 50 ? "blastn-short" : "megablast";
        }

        // temp dir + query file
        Path tmpDir = Path.of(config.getTempDir());
        Files.createDirectories(tmpDir);
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Path queryFile = tmpDir.resolve("blast_query_" + uid + ".fa");
        try {
            Files.writeString(queryFile, ">query\n" + cleanedSeq + "\n");

            List<String> cmd = new ArrayList<>(List.of(
                    config.getExecutable(),
                    "-query", queryFile.toString(),
                    "-db", config.getDbPath(),
                    "-task", task,
                    "-outfmt", "6 qseqid sseqid pident length mismatch gapopen qstart qend sstart send evalue bitscore qcovhsp",
                    "-max_target_seqs", String.valueOf(maxTargetSeqs),
                    "-max_hsps", String.valueOf(maxHsps),
                    "-evalue", String.valueOf(evalueCutoff),
                    "-dust", "yes",
                    "-num_threads", String.valueOf(config.getThreads())
            ));

            log.info("Running BLAST: {}", String.join(" ", cmd));
            long blastStart = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process proc = pb.start();

            CompletableFuture<List<String>> stdout = CompletableFuture.supplyAsync(
                    () -> readLines(proc.getInputStream()));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                    () -> readText(proc.getErrorStream()));

            long timeoutSec = config.getTimeoutSeconds();
            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new IllegalStateException("BLAST timed out after " + timeoutSec + " seconds.");
            }
            List<String> lines = stdout.join();
            String stderr = stderrFuture.join();
            int exitCode = proc.exitValue();
            long blastMs = System.currentTimeMillis() - blastStart;
            log.info("BLAST completed in {} ms ({} hits)", blastMs, lines.size());
            if (exitCode != 0) {
                log.error("BLAST exit code {}: {}", exitCode, stderr);
                throw new IllegalStateException("BLAST failed (exit " + exitCode + "). " + stderr);
            }

            return parseBlastOutput(lines);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("BLAST was interrupted.", e);
        } finally {
            try { Files.deleteIfExists(queryFile); } catch (IOException ignored) {}
        }
    }

    List<BlastHitDto> parseBlastOutput(List<String> lines) {
        List<BlastHitDto> hits = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] cols = line.split("\t");
            if (cols.length < 13) continue;

            BlastHitDto h = new BlastHitDto();
            String chr = cols[1];
            if (!chr.startsWith("chr") && chr.matches("^[0-9XYM]+$")) {
                chr = "chr" + chr;
            }
            h.setChromosome(chr);
            long sstart = Long.parseLong(cols[8]);
            long send = Long.parseLong(cols[9]);
            h.setStart(Math.min(sstart, send));
            h.setEnd(Math.max(sstart, send));
            h.setBedStart(Math.max(0L, h.getStart() - 1L));
            h.setBedEnd(h.getEnd());
            h.setStrand(sstart <= send ? "+" : "-");
            h.setIdentity(Double.parseDouble(cols[2]));
            h.setQueryCoverage(Double.parseDouble(cols[12]));
            h.setAlignLen(Integer.parseInt(cols[3]));
            h.setMismatch(Integer.parseInt(cols[4]));
            h.setGapOpen(Integer.parseInt(cols[5]));
            h.setQStart(Long.parseLong(cols[6]));
            h.setQEnd(Long.parseLong(cols[7]));
            h.setEvalue(cols[10]);
            h.setBitScore(Double.parseDouble(cols[11]));
            hits.add(h);
        }
        // Stable scientific ranking. Mapping qualification/ambiguity is applied
        // later because it depends on the user-selected coverage threshold.
        hits.sort(Comparator.comparingDouble(BlastHitDto::getBitScore).reversed()
                .thenComparingDouble(h -> parseEvalue(h.getEvalue()))
                .thenComparing(Comparator.comparingDouble(BlastHitDto::getQueryCoverage).reversed())
                .thenComparing(Comparator.comparingDouble(BlastHitDto::getIdentity).reversed())
                .thenComparing(BlastHitDto::getChromosome)
                .thenComparingLong(BlastHitDto::getStart)
                .thenComparingLong(BlastHitDto::getEnd));
        for (int i = 0; i < hits.size(); i++) {
            hits.get(i).setRank(i + 1);
        }
        return hits;
    }

    private static double parseEvalue(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException e) { return Double.MAX_VALUE; }
    }

    private static List<String> readLines(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readText(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
