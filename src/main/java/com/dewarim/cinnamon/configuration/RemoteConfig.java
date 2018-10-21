package com.dewarim.cinnamon.configuration;

public class RemoteConfig {

    private String  hostname   = "localhost";
    private String  protocol   = "http";
    private Integer port       = 8080;
    private String  contentUrl = "/cinnamon/osd/getContentXml";
    private String  currentUrl = "/cinnamon/osd/isCurrent";
    private String  existsUrl  = "/cinnamon/osd/exists";

    public RemoteConfig() {
    }

    public String generateGetContentUrl() {
        return String.format("%s://%s:%d%s", protocol, hostname, port, contentUrl);
    }

    public String generateIsCurrentUrl() {
        return String.format("%s://%s:%d%s", protocol, hostname, port, currentUrl);
    }

    public String generateExistsUrl() {
        return String.format("%s://%s:%d%s", protocol, hostname, port, existsUrl);
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public String getExistsUrl() {
        return existsUrl;
    }

    public void setExistsUrl(String existsUrl) {
        this.existsUrl = existsUrl;
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
