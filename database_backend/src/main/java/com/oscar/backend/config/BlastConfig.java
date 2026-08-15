package com.oscar.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oscar.blast")
public class BlastConfig {

    private boolean enabled = false;
    private String executable = "blastn";
    private String dbPath = "/data/oscar/reference/hg38/blastdb/hg38";
    private String tempDir = "/tmp/oscar-blast";
    private int threads = 4;
    private int maxTargetSeqs = 20;
    private int maxHsps = 200;          // per query-subject pair, user-overridable
    // Keep a generous process window for a valid near-20 kb hg38 query under
    // concurrent production load. This remains configurable for deployments.
    private long timeoutSeconds = 3_600;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public String getExecutable() { return executable; }
    public void setExecutable(String v) { this.executable = v; }

    public String getDbPath() { return dbPath; }
    public void setDbPath(String v) { this.dbPath = v; }

    public String getTempDir() { return tempDir; }
    public void setTempDir(String v) { this.tempDir = v; }

    public int getThreads() { return threads; }
    public void setThreads(int v) { this.threads = v; }

    public int getMaxTargetSeqs() { return maxTargetSeqs; }
    public void setMaxTargetSeqs(int v) { this.maxTargetSeqs = v; }

    public int getMaxHsps() { return maxHsps; }
    public void setMaxHsps(int v) { this.maxHsps = v; }

    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long v) { this.timeoutSeconds = v; }
}
