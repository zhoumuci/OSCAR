package com.oscar.backend.service;

@FunctionalInterface
public interface PeakGeneContextProgressListener {

    PeakGeneContextProgressListener NOOP = (progress, stage, message) -> { };

    void update(int progress, String stage, String message);
}
