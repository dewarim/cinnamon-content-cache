package com.dewarim.cinnamon.test.integration;


import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.UrlMapping;
import com.dewarim.cinnamon.application.servlet.TestServlet;
import com.dewarim.cinnamon.configuration.CinnamonConfig;
import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.model.request.ContentRequest;
import com.dewarim.cinnamon.model.response.GenericResponse;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.dewarim.cinnamon.application.servlet.TestServlet.GENERIC_RESPONSE;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class ContentServletIntegrationTest extends CinnamonIntegrationTest {

    CinnamonConfig config;
    RemoteConfig   remoteConfig;
    ObjectMapper   mapper = new XmlMapper();

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
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        GenericResponse genericResponse = mapper.readValue(response.getEntity().getContent(), GenericResponse.class);
        assertEquals(GENERIC_RESPONSE, genericResponse);
    }

    @Test
    public void getNewContentFromCache() throws IOException {
        TestServlet.isCurrent = true;
        Long id = 2L;

        Path tempDir = Files.createTempDirectory("cinnamon-content-cache-test-");
        config.getServerConfig().setDataRoot(tempDir.toFile().getAbsolutePath());
        FileSystemContentProvider contentProvider = new FileSystemContentProvider();
        FileInputStream           inputStream     = new FileInputStream("pom.xml");
        ContentMeta               contentMeta     = new ContentMeta();
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
        byte[] expectedBytes = Files.readAllBytes(Paths.get("pom.xml"));
        byte[] actualBytes = response.getEntity().getContent().readAllBytes();
        assertEquals(new String(expectedBytes),new String(actualBytes));
    }

    @Test
    public void getContentWithInvalidRequest() throws IOException{
        TestServlet.hasContent = true;
        ContentRequest contentRequest = new ContentRequest(ticket, 0L);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.INVALID_REQUEST);
    }

    @Test
    public void getContentWhichDoesNotExist() throws IOException{
        TestServlet.hasContent = false;
        ContentRequest contentRequest = new ContentRequest(ticket, Long.MAX_VALUE);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.IO_EXCEPTION, SC_NOT_FOUND);
    }

}
