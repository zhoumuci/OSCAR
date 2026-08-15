package com.oscar.backend.service;

@FunctionalInterface
public interface SequenceAnalysisProgressListener {

    SequenceAnalysisProgressListener NOOP = (progress, stage, message) -> { };

    void update(int progress, String stage, String message);
}
