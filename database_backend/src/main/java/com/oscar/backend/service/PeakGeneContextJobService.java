package com.oscar.backend.service;

import com.oscar.backend.entity.PeakGeneContextJobResponse;
import com.oscar.backend.entity.PeakGeneContextRequest;
import com.oscar.backend.entity.PeakGeneContextResponse;
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
public class PeakGeneContextJobService implements DisposableBean {

    private static final int MAX_QUEUED_JOBS = 16;
    private static final Duration FINISHED_JOB_TTL = Duration.ofMinutes(30);

    private final PeakGeneContextService analysisService;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,
            2,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_JOBS),
            runnable -> {
                Thread thread = new Thread(runnable, "peak-gene-context-worker");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    public PeakGeneContextJobService(PeakGeneContextService analysisService) {
        this.analysisService = analysisService;
    }

    public PeakGeneContextJobResponse submit(PeakGeneContextRequest request) {
        cleanupExpiredJobs();
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId);
        jobs.put(jobId, state);
        try {
            executor.execute(() -> runJob(state, request));
        } catch (RuntimeException error) {
            jobs.remove(jobId);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "The Peak-to-Gene analysis queue is full."
            );
        }
        return state.snapshot();
    }

    public PeakGeneContextJobResponse get(String jobId) {
        cleanupExpiredJobs();
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Peak-to-Gene analysis job not found or expired.");
        }
        return state.snapshot();
    }

    private void runJob(JobState state, PeakGeneContextRequest request) {
        state.start();
        try {
            PeakGeneContextResponse result = analysisService.analyze(request, state::updateProgress);
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
        return error.toString();
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
        private String message = "Waiting for a Peak-to-Gene query worker.";
        private String error;
        private Instant updatedAt = createdAt;
        private PeakGeneContextResponse result;

        private JobState(String jobId) {
            this.jobId = jobId;
        }

        private synchronized void start() {
            status = "RUNNING";
            progress = 5;
            stage = "STARTING";
            message = "Starting Peak-to-Gene linkage analysis.";
            updatedAt = Instant.now();
        }

        private synchronized void updateProgress(int value, String nextStage, String nextMessage) {
            if (!"RUNNING".equals(status)) return;
            progress = Math.max(progress, Math.min(99, value));
            stage = nextStage;
            message = nextMessage;
            updatedAt = Instant.now();
        }

        private synchronized void complete(PeakGeneContextResponse response) {
            status = "COMPLETED";
            progress = 100;
            stage = "COMPLETED";
            message = "Peak-to-Gene linkage analysis completed.";
            result = response;
            updatedAt = Instant.now();
        }

        private synchronized void fail(String failureMessage) {
            status = "FAILED";
            stage = "FAILED";
            message = "Peak-to-Gene linkage analysis failed.";
            error = failureMessage;
            updatedAt = Instant.now();
        }

        private synchronized boolean isFinishedBefore(Instant cutoff) {
            return ("COMPLETED".equals(status) || "FAILED".equals(status)) && updatedAt.isBefore(cutoff);
        }

        private synchronized PeakGeneContextJobResponse snapshot() {
            PeakGeneContextJobResponse response = new PeakGeneContextJobResponse();
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
