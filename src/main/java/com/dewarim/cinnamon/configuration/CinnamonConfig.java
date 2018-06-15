package com.dewarim.cinnamon.configuration;

import java.util.ArrayList;
import java.util.List;

public class CinnamonConfig {
    
    private ServerConfig serverConfig = new ServerConfig();

    @Deprecated(forRemoval = true)
    private String logbackLoggingConfigPath = "/opt/cinnamon/logback.xml";
        
    private String systemAdministratorEmail;
    private MailConfig mailConfig = new MailConfig();
    private RemoteConfig remoteConfig = new RemoteConfig();

    public String getLogbackLoggingConfigPath() {
        return logbackLoggingConfigPath;
    }

    public void setLogbackLoggingConfigPath(String logbackLoggingConfigPath) {
        this.logbackLoggingConfigPath = logbackLoggingConfigPath;
    }

    public String getSystemAdministratorEmail() {
        return systemAdministratorEmail;
    }

    public void setSystemAdministratorEmail(String systemAdministratorEmail) {
        this.systemAdministratorEmail = systemAdministratorEmail;
    }

    public MailConfig getMailConfig() {
        return mailConfig;
    }

    public void setMailConfig(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }
}
