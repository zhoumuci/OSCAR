package com.oscar.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oscar")
public class BedtoolsProperties {

    private String resourceRoot = "/data/oscar";
    private final Bedtools bedtools = new Bedtools();

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    public Bedtools getBedtools() {
        return bedtools;
    }

    public static class Bedtools {

        private String referenceRoot = "/data/oscar/reference";
        private String sampleTrackRoot = "/data/oscar/sample_tracks";
        private String tmpRoot = "/data/oscar/tmp/bedtools";
        private String binaryPath = "/usr/bin/bedtools";
        private long maxRegionBp = 5_000_000L;
        private long timeoutSeconds = 120L;

        public String getReferenceRoot() {
            return referenceRoot;
        }

        public void setReferenceRoot(String referenceRoot) {
            this.referenceRoot = referenceRoot;
        }

        public String getSampleTrackRoot() {
            return sampleTrackRoot;
        }

        public void setSampleTrackRoot(String sampleTrackRoot) {
            this.sampleTrackRoot = sampleTrackRoot;
        }

        public String getTmpRoot() {
            return tmpRoot;
        }

        public void setTmpRoot(String tmpRoot) {
            this.tmpRoot = tmpRoot;
        }

        public String getBinaryPath() {
            return binaryPath;
        }

        public void setBinaryPath(String binaryPath) {
            this.binaryPath = binaryPath;
        }

        public long getMaxRegionBp() {
            return maxRegionBp;
        }

        public void setMaxRegionBp(long maxRegionBp) {
            this.maxRegionBp = maxRegionBp;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
