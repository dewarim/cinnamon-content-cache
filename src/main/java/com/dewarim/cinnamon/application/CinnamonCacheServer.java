package com.dewarim.cinnamon.application;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.dewarim.cinnamon.application.servlet.*;
import com.dewarim.cinnamon.configuration.CinnamonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.eclipse.jetty.annotations.AnnotationDecorator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 */
public class CinnamonCacheServer {

    private       int            port;
    private       Server         server;
    private       WebAppContext  webAppContext = new WebAppContext();
    public static CinnamonConfig config        = new CinnamonConfig();
    private       Thread         reaperThread;

    public CinnamonCacheServer() {
    }

    public CinnamonCacheServer(int port) {
        this.port = port;
        server = new Server(port);
        webAppContext.setContextPath("/");
        webAppContext.setResourceBase(".");
        webAppContext.getObjectFactory().addDecorator(new AnnotationDecorator(webAppContext));

        addServlets(webAppContext);

        server.setHandler(webAppContext);


    }

    public void start() throws Exception {
        server.start();
    }

    private void addServlets(WebAppContext handler) {
        handler.addServlet(ContentServlet.class, "/content/*");
    }

    public static void main(String[] args) throws Exception {
        Args       cliArguments = new Args();
        JCommander commander    = JCommander.newBuilder().addObject(cliArguments).build();
        commander.parse(args);

        if ((cliArguments.help)) {
            commander.setColumnSize(80);
            commander.usage();
            return;
        }

        if (cliArguments.writeConfigFile != null) {
            writeConfig(cliArguments.writeConfigFile);
            return;
        }

        if (cliArguments.configFilename != null) {
            config = readConfig(cliArguments.configFilename);
        }

        if (cliArguments.port != null) {
            config.getServerConfig().setPort(cliArguments.port);
        }

        CinnamonCacheServer server = new CinnamonCacheServer(config.getServerConfig().getPort());
        server.start();
        server.getServer().join();
    }

    public Server getServer() {
        return server;
    }

    public static void writeConfig(String filename) {
        File configFile = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            ObjectMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
            xmlMapper.writeValue(fos, config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static CinnamonConfig readConfig(String filename) {
        File configFile = new File(filename);
        try (FileInputStream fis = new FileInputStream(configFile)) {
            ObjectMapper xmlMapper = new XmlMapper();
            return xmlMapper.readValue(fis, CinnamonConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Args {
        @Parameter(names = {"--port", "-p"}, description = "Port on which the server listens. Default is 9090.")
        Integer port;

        @Parameter(names = "--write-config", description = "Write the default configuration to this file")
        String writeConfigFile;

        @Parameter(names = {"--config", "-c"}, description = "Where to load the configuration file from")
        String configFilename;

        @Parameter(names = {"--help", "-h"}, help = true, description = "Display help text.")
        boolean help;
    }

    public WebAppContext getWebAppContext() {
        return webAppContext;
    }

    public static void setConfig(CinnamonConfig config) {
        CinnamonCacheServer.config = config;
    }
}
