package com.dewarim.cinnamon.test.integration;

import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.Reaper;
import com.dewarim.cinnamon.application.servlet.TestServlet;
import com.dewarim.cinnamon.configuration.CinnamonConfig;
import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReaperIntegrationTest extends CinnamonIntegrationTest {

    private static final Logger log = LogManager.getLogger(ReaperIntegrationTest.class);


    CinnamonConfig config;
    Path           tempDir;
    RemoteConfig   remoteConfig;

    @Before
    public void setup() throws IOException {
        config = CinnamonCacheServer.config;
        tempDir = Files.createTempDirectory("cinnamon-content-cache-test-");
        config.getServerConfig().setDataRoot(tempDir.toFile().getAbsolutePath());
        remoteConfig = config.getRemoteConfig();
        remoteConfig.setExistsUrl("/test/exists");
        remoteConfig.setPort(cinnamonTestPort);

    }

    @Test
    public void reaperTest() throws IOException {
        String      dataRoot    = CinnamonCacheServer.config.getServerConfig().getDataRoot();
        ContentMeta contentMeta = ContentServletIntegrationTest.createContentMeta(config, 999L, Paths.get(dataRoot));
        TestServlet.nonExistingId = contentMeta.getId().toString();
        log.debug("created contentMeta {}", contentMeta);

        ContentMeta doNotDelete = ContentServletIntegrationTest.createContentMeta(config, 222L, Paths.get(dataRoot));

        FileSystemContentProvider contentProvider = new FileSystemContentProvider();
        Reaper                    reaper          = new Reaper(dataRoot, remoteConfig, contentProvider);
        reaper.run();
        File                  contentFile = contentProvider.getContentFile(contentMeta);
        Optional<ContentMeta> deletedMeta = contentProvider.getContentMeta(contentMeta.getId());
        assertFalse(contentFile.exists());
        assertFalse(deletedMeta.isPresent());

        File notDeleted = contentProvider.getContentFile(doNotDelete);
        Optional<ContentMeta> notDeletedMeta = contentProvider.getContentMeta(doNotDelete.getId());
        assertTrue(notDeleted.exists());
        assertTrue(notDeletedMeta.isPresent());

    }


}
