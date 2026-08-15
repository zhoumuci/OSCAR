package com.oscar.backend.service;

import com.oscar.backend.entity.SequencePeak2GeneJobResponse;
import com.oscar.backend.entity.SequencePeak2GeneRequest;
import com.oscar.backend.entity.SequencePeak2GeneResponse;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class SequencePeak2GeneJobService implements DisposableBean {

    private static final int MAX_QUEUED_JOBS = 12;
    private static final Duration FINISHED_JOB_TTL = Duration.ofMinutes(30);

    private final AnalysisService analysisService;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_JOBS),
            runnable -> {
                Thread thread = new Thread(runnable, "sequence-blast-worker");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    public SequencePeak2GeneJobService(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public SequencePeak2GeneJobResponse submit(SequencePeak2GeneRequest request) {
        cleanupExpiredJobs();
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId);
        jobs.put(jobId, state);
        try {
            executor.execute(() -> runJob(state, request));
        } catch (RuntimeException e) {
            jobs.remove(jobId);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "The BLAST queue is full. Please retry after an existing analysis finishes."
            );
        }
        return state.snapshot();
    }

    public SequencePeak2GeneJobResponse get(String jobId) {
        cleanupExpiredJobs();
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sequence analysis job not found or expired.");
        }
        return state.snapshot();
    }

    int queueSize() {
        return executor.getQueue().size();
    }

    private void runJob(JobState state, SequencePeak2GeneRequest request) {
        state.start();
        try {
            SequencePeak2GeneResponse result = analysisService.sequencePeak2Gene(
                    request,
                    state::updateProgress
            );
            state.complete(result);
        } catch (Throwable error) {
            state.fail(errorMessage(error));
        }
    }

    private void cleanupExpiredJobs() {
        Instant cutoff = Instant.now().minus(FINISHED_JOB_TTL);
        jobs.entrySet().removeIf(entry -> entry.getValue().isFinishedBefore(cutoff));
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof ResponseStatusException status && status.getReason() != null) {
            return status.getReason();
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? "Sequence mapping failed." : message;
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }

    private static final class JobState {
        private final String jobId;
        private final Instant createdAt = Instant.now();
        private String status = "QUEUED";
        private int progress = 3;
        private String stage = "QUEUED";
        private String message = "Waiting for the BLAST worker.";
        private String error;
        private Instant updatedAt = createdAt;
        private SequencePeak2GeneResponse result;

        private JobState(String jobId) {
            this.jobId = jobId;
        }

        private synchronized void start() {
            status = "RUNNING";
            progress = Math.max(progress, 5);
            stage = "STARTING";
            message = "Starting sequence analysis.";
            updatedAt = Instant.now();
        }

        private synchronized void updateProgress(int value, String nextStage, String nextMessage) {
            if (!"RUNNING".equals(status)) return;
            progress = Math.max(progress, Math.min(99, Math.max(0, value)));
            stage = nextStage;
            message = nextMessage;
            updatedAt = Instant.now();
        }

        private synchronized void complete(SequencePeak2GeneResponse response) {
            status = "COMPLETED";
            progress = 100;
            stage = "COMPLETED";
            message = "Sequence analysis completed.";
            result = response;
            updatedAt = Instant.now();
        }

        private synchronized void fail(String failureMessage) {
            status = "FAILED";
            stage = "FAILED";
            message = "Sequence analysis failed.";
            error = failureMessage;
            updatedAt = Instant.now();
        }

        private synchronized boolean isFinishedBefore(Instant cutoff) {
            return ("COMPLETED".equals(status) || "FAILED".equals(status)) && updatedAt.isBefore(cutoff);
        }

        private synchronized SequencePeak2GeneJobResponse snapshot() {
            SequencePeak2GeneJobResponse response = new SequencePeak2GeneJobResponse();
            response.setJobId(jobId);
            response.setStatus(status);
            response.setProgress(progress);
            response.setStage(stage);
            response.setMessage(message);
            response.setError(error);
            response.setCreatedAt(createdAt);
            response.setUpdatedAt(updatedAt);
            response.setResult(result);
            return response;
        }
    }
}
