package com.dewarim.cinnamon.test.integration;


import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.UrlMapping;
import com.dewarim.cinnamon.application.servlet.TestServlet;
import com.dewarim.cinnamon.configuration.CinnamonConfig;
import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.model.request.ContentRequest;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class ContentServletIntegrationTest extends CinnamonIntegrationTest {

    CinnamonConfig config;
    RemoteConfig   remoteConfig;

    @Before
    public void setup() {
        config = CinnamonCacheServer.config;
        remoteConfig = config.getRemoteConfig();
        remoteConfig.setContentUrl("/test/getContent");
        remoteConfig.setCurrentUrl("/test/isCurrent");
        remoteConfig.setPort(cinnamonTestPort);
    }

    @Test
    public void getNewUncachedContent() throws IOException {
        TestServlet.isCurrent = false;
        Long           id             = 1L;
        ContentRequest contentRequest = new ContentRequest(ticket, id);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        StatusLine     statusLine     = response.getStatusLine();
        int            statusCode     = statusLine.getStatusCode();
        assertEquals(SC_OK, statusCode);

        // TODO: check returned content
    }

    @Test
    public void getNewContentFromCache() throws IOException {
        TestServlet.isCurrent = true;
        Long id = 2L;

        Path tempDir = Files.createTempDirectory("cinnamon-content-cache-test-");
        config.getServerConfig().setDataRoot(tempDir.toFile().getAbsolutePath());
        FileSystemContentProvider contentProvider = new FileSystemContentProvider();
        FileInputStream           inputStream = new FileInputStream("pom.xml");
        ContentMeta contentMeta = new ContentMeta();
        contentMeta.setName("test-content");
        contentMeta.setContentType("text/plain");
        contentMeta.setId(id);
        contentMeta.setContentHash("ignored by test servlet");
        ContentMeta meta = contentProvider.writeContentStream(contentMeta, inputStream);

        ContentRequest contentRequest = new ContentRequest(ticket, id);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        StatusLine     statusLine     = response.getStatusLine();
        int            statusCode     = statusLine.getStatusCode();
        assertEquals(SC_OK, statusCode);

        // TODO: check returned content
    }

    // TODO: test unhappy paths

}
