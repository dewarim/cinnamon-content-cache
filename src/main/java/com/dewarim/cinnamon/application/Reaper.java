package com.dewarim.cinnamon.application;

import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Periodically walk the cache store's file tree and
 * check all objects if they still exist on the master server.
 */
public class Reaper implements Runnable {

    private              String                    dataRootPath    = CinnamonCacheServer.config.getServerConfig().getDataRoot();
    private static final Logger                    log             = LogManager.getLogger(Reaper.class);
    private              RemoteConfig              remoteConfig    = CinnamonCacheServer.config.getRemoteConfig();
    private              FileSystemContentProvider contentProvider = new FileSystemContentProvider();

    public Reaper() {
    }

    public Reaper(String dataRootPath, RemoteConfig remoteConfig, FileSystemContentProvider contentProvider) {
        this.dataRootPath = dataRootPath;
        this.remoteConfig = remoteConfig;
        this.contentProvider = contentProvider;
    }

    @Override
    public void run() {
        FileVisitor<Path> fileVisitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                log.debug("Reaper visits file " + file.toAbsolutePath());
                if (file.toAbsolutePath().toString().endsWith(".xml")) {
                    Optional<ContentMeta> contentMetaOpt = readContentMeta(file);
                    if (!contentMetaOpt.isPresent()) {
                        return FileVisitResult.CONTINUE;
                    }
                    ContentMeta contentMeta = contentMetaOpt.get();
                    String      existsUrl   = remoteConfig.generateExistsUrl();
                    HttpResponse httpResponse = Request.Post(existsUrl)
                            .bodyForm(Form.form()
                                    .add("id", contentMeta.getId().toString())
                                    .add("contentHash", contentMeta.getContentHash())
                                    .build()
                            )
                            .execute().returnResponse();
                    StatusLine statusLine = httpResponse.getStatusLine();
                    int        statusCode = statusLine.getStatusCode();
                    switch (statusCode) {
                        case SC_OK:
                            log.debug("OSD {} still exists.", contentMeta.getId(), contentMeta.getContentHash());
                            return FileVisitResult.CONTINUE;
                        case SC_NO_CONTENT:
                            boolean deleteSuccess = file.toFile().delete();
                            if (!deleteSuccess) {
                                log.debug("OSD {} no longer exists, but failed to delete contentMeta.", contentMeta.getId(), contentMeta.getContentHash());
                                return FileVisitResult.CONTINUE;
                            }
                            boolean deleteContentSuccess = contentProvider.getContentFile(contentMeta).delete();
                            if (!deleteContentSuccess) {
                                log.debug("OSD {} no longer exists, but failed to delete content.", contentMeta.getId(), contentMeta.getContentHash());
                                return FileVisitResult.CONTINUE;
                            }
                            log.info("Deleted obsolete OSD {} from cache.", contentMeta.getId());
                            break;
                        default:
                            log.debug("Unexpected response code for osd::exists(id): {}", statusCode);
                            return FileVisitResult.CONTINUE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };

        while (!Thread.interrupted()) {
            try {
                log.debug("Reaper has awakened, looking at: {}", dataRootPath);
                Files.walkFileTree(Paths.get(dataRootPath), fileVisitor);
                long reaperIntervalInMillis = getReaperIntervalInMillis();
                log.debug("Reaper is going to sleep for {} ms", reaperIntervalInMillis);
                // sleep for 5 minutes (default):
                Thread.sleep(reaperIntervalInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (NoSuchFileException ignore) {
                /*
                 * Ignored, happens when Reaper deletes the current file (contentMeta) and the
                 * next file (actual content) and then the FileWalker tries to visit the content file.
                 */

            } catch (Exception e) {
                log.warn("Reaper thread encountered an exception (ignored).", e);
            }
        }

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

    private long getReaperIntervalInMillis() {
        return CinnamonCacheServer.config.getServerConfig().getReaperIntervalInMillis();
    }

}
