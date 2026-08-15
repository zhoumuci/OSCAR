package com.oscar.backend.service;

import com.oscar.backend.entity.SequencePeak2GeneJobResponse;
import com.oscar.backend.entity.SequencePeak2GeneRequest;
import com.oscar.backend.entity.SequencePeak2GeneResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class SequencePeak2GeneJobServiceTest {

    @Test
    void runsBlastWorkOffRequestThreadAndExposesPollableCompletion() {
        AnalysisService analysisService = mock(AnalysisService.class);
        SequencePeak2GeneResponse expected = new SequencePeak2GeneResponse();
        doAnswer(invocation -> {
            SequenceAnalysisProgressListener progress = invocation.getArgument(1);
            progress.update(18, "BLASTING", "Searching hg38.");
            progress.update(84, "QUERYING_EVIDENCE", "Retrieving evidence.");
            return expected;
        }).when(analysisService).sequencePeak2Gene(
                any(SequencePeak2GeneRequest.class),
                any(SequenceAnalysisProgressListener.class)
        );

        SequencePeak2GeneJobService jobs = new SequencePeak2GeneJobService(analysisService);
        try {
            SequencePeak2GeneJobResponse submitted = jobs.submit(new SequencePeak2GeneRequest());
            assertNotNull(submitted.getJobId());

            SequencePeak2GeneJobResponse completed = assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> awaitFinished(jobs, submitted.getJobId())
            );
            assertEquals("COMPLETED", completed.getStatus());
            assertEquals(100, completed.getProgress());
            assertEquals(expected, completed.getResult());
        } finally {
            jobs.destroy();
        }
    }

    private static SequencePeak2GeneJobResponse awaitFinished(
            SequencePeak2GeneJobService jobs,
            String jobId
    ) throws InterruptedException {
        SequencePeak2GeneJobResponse current = jobs.get(jobId);
        while ("QUEUED".equals(current.getStatus()) || "RUNNING".equals(current.getStatus())) {
            Thread.sleep(5L);
            current = jobs.get(jobId);
        }
        return current;
    }
}
