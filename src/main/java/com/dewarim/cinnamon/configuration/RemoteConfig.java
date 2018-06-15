package com.dewarim.cinnamon.configuration;

public class RemoteConfig {

    private String  hostname   = "localhost";
    private String  protocol   = "http";
    private Integer port       = 8080;
    private String  contentUrl = "/cinnamon/cinnamon/legacy?command=getcontent";

    public RemoteConfig() {
    }

    public String generateUrl() {
        return String.format("%s://%s:%d%s", protocol, hostname, port, contentUrl);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    @Override
    public String toString() {
        return "RemoteConfig{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", contentUrl='" + contentUrl + '\'' +
                '}';
    }
}
