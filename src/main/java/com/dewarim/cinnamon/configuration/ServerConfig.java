package com.dewarim.cinnamon.configuration;

public class ServerConfig {

    /**
     * Synchronization object so the integration test can change the sleep time of the reaper thread
     * on the fly.
     */
    private final Object REAPER_LOCK = new Object();

    private int port = 9090;
    private String systemRoot = "/opt/cinnamon/cinnamon-system";
    private String dataRoot = "/opt/cinnamon/cinnamon-cache";
    private long reaperIntervalInMillis = 300_000;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(String systemRoot) {
        this.systemRoot = systemRoot;
    }

    public String getDataRoot() {
        return dataRoot;
    }

    public void setDataRoot(String dataRoot) {
        this.dataRoot = dataRoot;
    }

    public long getReaperIntervalInMillis() {
        synchronized (REAPER_LOCK) {
            return reaperIntervalInMillis;
        }
    }

    public void setReaperIntervalInMillis(long reaperIntervalInMillis) {
        synchronized (REAPER_LOCK) {
            this.reaperIntervalInMillis = reaperIntervalInMillis;
        }
    }
}
