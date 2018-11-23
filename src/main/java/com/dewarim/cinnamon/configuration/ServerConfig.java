package com.dewarim.cinnamon.configuration;

public class ServerConfig {

    private int    port       = 9090;
    private String systemRoot = "/opt/cinnamon/cinnamon-system";
    private String dataRoot   = "/opt/cinnamon/cinnamon-cache";

    /**
     * How long to wait until lock acquisition for reading/writing content fails.
     */
    private long lockAcquisitionTimeoutMillis = 300_000L;

    /**
     * How long to wait before checking again if a user request can acquire a lock on a resource
     */
    private long lockAcquisitionCheckPeriodMillis = 1000L;

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

    public long getLockAcquisitionTimeoutMillis() {
        return lockAcquisitionTimeoutMillis;
    }

    public void setLockAcquisitionTimeoutMillis(long lockAcquisitionTimeoutMillis) {
        this.lockAcquisitionTimeoutMillis = lockAcquisitionTimeoutMillis;
    }

    public long getLockAcquisitionCheckPeriodMillis() {
        return lockAcquisitionCheckPeriodMillis;
    }

    public void setLockAcquisitionCheckPeriodMillis(long lockAcquisitionCheckPeriodMillis) {
        this.lockAcquisitionCheckPeriodMillis = lockAcquisitionCheckPeriodMillis;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", systemRoot='" + systemRoot + '\'' +
                ", dataRoot='" + dataRoot + '\'' +
                ", lockAcquisitionTimeoutMillis=" + lockAcquisitionTimeoutMillis +
                ", lockAcquisitionCheckPeriodMillis=" + lockAcquisitionCheckPeriodMillis +
                '}';
    }
}
