package com.dewarim.cinnamon.application;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dewarim.cinnamon.application.CinnamonCacheServer.readConfig;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Walk the cache store's file tree and
 * check all objects if they still exist on the master server.
 */
public class Reaper {

    private              String                    dataRootPath    = CinnamonCacheServer.config.getServerConfig().getDataRoot();
    private static final Logger                    log             = LogManager.getLogger(Reaper.class);
    private              RemoteConfig              remoteConfig    = CinnamonCacheServer.config.getRemoteConfig();
    private              FileSystemContentProvider contentProvider = new FileSystemContentProvider();
    private              ObjectMapper              objectMapper    = new XmlMapper();

    public Reaper() {
    }

    public Reaper(String dataRootPath, RemoteConfig remoteConfig, FileSystemContentProvider contentProvider) {
        this.dataRootPath = dataRootPath;
        this.remoteConfig = remoteConfig;
        this.contentProvider = contentProvider;
    }

    public void run() throws IOException {
        final List<Optional<ContentMeta>> cacheMetadata = new ArrayList<>();
        FileVisitor<Path> fileCounter = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toAbsolutePath().toString().endsWith(".xml")) {
                    cacheMetadata.add(readContentMeta(file));
                }
                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(Paths.get(dataRootPath), fileCounter);

        List<ContentMeta> validContentFiles = cacheMetadata.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        log.info("Found {} objects to check.", validContentFiles.size());

        final List<Long> ids = validContentFiles.stream().map(ContentMeta::getId).collect(Collectors.toList());

        int idCount = ids.size();
        int batchSize = (idCount / Math.max(1000, idCount / 1000)) + 1;

        final List<Long> removedIds = new ArrayList<>();
        // ids list will shrink due to entries being deleted via sublist() + clear().
        while (ids.size() > 0) {
            String         existsUrl    = remoteConfig.generateExistsUrl();
            CinnamonIdList idList       = new CinnamonIdList();
            int            subListSize  = Math.min(batchSize, ids.size());
            List<Long>     currentBatch = ids.subList(0, subListSize);
            log.debug("currentBatch size: " + currentBatch.size());
            idList.setIds(currentBatch);
            idList.setAccessToken(remoteConfig.getReaperAccessToken());
            try {
                String idsXml = objectMapper.writeValueAsString(idList);
                log.debug("sending: " + idsXml);
                HttpResponse httpResponse = Request.Post(existsUrl)
                        .bodyForm(Form.form()
                                // TODO: for Cinnamon 4, send straight xml instead of form.
                                .add("ids", idsXml)
                                .build()
                        )
                        .execute().returnResponse();

                StatusLine statusLine = httpResponse.getStatusLine();
                int        statusCode = statusLine.getStatusCode();
                switch (statusCode) {
                    case SC_OK:
                        String response = new String(httpResponse.getEntity().getContent().readAllBytes(), Charset.forName("UTF-8"));
                        log.debug("response:\n" + response);
                        CinnamonIdList cinnamonIdList = objectMapper.readValue(response, CinnamonIdList.class);
                        if (cinnamonIdList != null && cinnamonIdList.getIds() != null) {
                            currentBatch.removeAll(cinnamonIdList.getIds());
                        }
                        removedIds.addAll(currentBatch);
                        break;
                    default:
                        InputStream inputStream = httpResponse.getEntity().getContent();
                        String body = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                        log.debug("Unexpected response code for osd::checkObjectsExist(ids): {} {}", statusCode, body);
                }
                currentBatch.clear();
                log.info("Remaining ids to check: {}",ids.size());
            } catch (Exception e) {
                log.warn("Failed to check cache files due to:", e);
                throw new RuntimeException(e);
            }

        }

        List<ContentMeta> toDelete = validContentFiles.stream().filter(contentMeta -> removedIds.contains(contentMeta.getId())).collect(Collectors.toList());
        log.info("Going to delete {} stale cache entries.",toDelete.size());
        toDelete.forEach(contentMeta -> {
            String metadataPath = contentMeta.getContentPath() + ".xml";
            log.debug("Delete metadata @ {}", metadataPath);
            boolean deleteSuccess = new File(metadataPath).delete();
            if (!deleteSuccess) {
                log.debug("OSD {} no longer exists, but failed to delete contentMeta.", contentMeta.getId(), contentMeta.getContentHash());
                return;
            }
            boolean deleteContentSuccess = contentProvider.getContentFile(contentMeta).delete();
            if (!deleteContentSuccess) {
                log.debug("OSD {} no longer exists, but failed to delete content.", contentMeta.getId(), contentMeta.getContentHash());
                return;
            }
        });

    }

    public static class CinnamonIdList {
        private String     accessToken;
        private List<Long> ids;

        @JacksonXmlElementWrapper(localName = "ids")
        @JacksonXmlProperty(localName = "id")
        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static void main(String[] args) {
        Reaper.Args cliArguments = new Reaper.Args();
        JCommander                      commander    = JCommander.newBuilder().addObject(cliArguments).build();
        commander.parse(args);

        if ((cliArguments.help)) {
            commander.setColumnSize(80);
            commander.usage();
            return;
        }

        if (cliArguments.configFilename != null) {
            CinnamonCacheServer.setConfig(readConfig(cliArguments.configFilename));
        }

        try {
            new Reaper().run();
        } catch (IOException e) {
            log.error("Reaper did not complete normally, encountered exception:", e);
            System.exit(1);
        }

    }

    private static class Args {
        @Parameter(names = {"--config", "-c"}, required = true, description = "Where to load the configuration file from")
        String configFilename;

        @Parameter(names = {"--help", "-h"}, help = true, description = "Display help text.")
        boolean help;
    }


    private Optional<ContentMeta> readContentMeta(Path file) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            log.debug("Reading contentMeta of: {}", file.toAbsolutePath());
            return Optional.of(objectMapper.readValue(new FileInputStream(file.toFile()), ContentMeta.class));
        } catch (Exception e) {
            log.debug("Could not load " + file.toAbsolutePath(), e);
            return Optional.empty();
        }
    }


}
