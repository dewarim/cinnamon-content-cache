package com.dewarim.cinnamon.test.integration;


import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.LockService;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.dewarim.cinnamon.application.servlet.TestServlet.GENERIC_RESPONSE;
import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentServletIntegrationTest extends CinnamonIntegrationTest {

    private static final Logger log = LogManager.getLogger(ContentServletIntegrationTest.class);


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
        createContentMeta(id);
        ContentRequest contentRequest = new ContentRequest(ticket, id);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        StatusLine     statusLine     = response.getStatusLine();
        int            statusCode     = statusLine.getStatusCode();
        assertEquals(SC_OK, statusCode);
        byte[] expectedBytes = Files.readAllBytes(Paths.get("pom.xml"));
        byte[] actualBytes   = response.getEntity().getContent().readAllBytes();
        assertEquals(new String(expectedBytes), new String(actualBytes));
    }

    @Test
    public void remoteContentIsNewerThanCached() throws IOException {
        TestServlet.isCurrent = false;
        Long           id             = 4L;
        ContentMeta    contentMeta    = createContentMeta(id);
        ContentRequest contentRequest = new ContentRequest(ticket, id);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        StatusLine     statusLine     = response.getStatusLine();
        int            statusCode     = statusLine.getStatusCode();
        assertEquals(SC_OK, statusCode);
        byte[] expectedBytes = Files.readAllBytes(Paths.get(contentMeta.getContentPath()));
        byte[] actualBytes   = response.getEntity().getContent().readAllBytes();
        assertEquals(new String(expectedBytes), new String(actualBytes));
    }

    @Test
    public void getContentWithInvalidRequest() throws IOException {
        TestServlet.hasContent = true;
        ContentRequest contentRequest = new ContentRequest(ticket, 0L);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.INVALID_REQUEST);
    }

    @Test
    public void getContentWhichDoesNotExist() throws IOException {
        TestServlet.hasContent = false;
        ContentRequest contentRequest = new ContentRequest(ticket, Long.MAX_VALUE);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.IO_EXCEPTION, SC_NOT_FOUND);
    }

    @Test
    public void handleInternalServerError() throws IOException {
        Long id = 3L;
        createContentMeta(id);
        ContentRequest contentRequest = new ContentRequest(ticket, id);

        RemoteConfig backupConfig = remoteConfig;
        CinnamonCacheServer.config.setRemoteConfig(null);
        HttpResponse response = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.INTERNAL_SERVER_ERROR_TRY_AGAIN_LATER, SC_INTERNAL_SERVER_ERROR);
        CinnamonCacheServer.config.setRemoteConfig(backupConfig);
    }

    @Test
    public void handleRemoteIOError() throws IOException {
        Long id = 3L;
        createContentMeta(id);
        ContentRequest contentRequest = new ContentRequest(ticket, id);

        CinnamonCacheServer.config.getRemoteConfig().setHostname("example.invalid");
        HttpResponse response = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.IO_EXCEPTION, SC_NOT_FOUND);
        CinnamonCacheServer.config.getRemoteConfig().setHostname("localhost");
    }

    @Test
    public void handleRemoteException() throws IOException {
        TestServlet.statusCode = SC_INTERNAL_SERVER_ERROR;
        Long id = 5L;
        createContentMeta(id);
        ContentRequest contentRequest = new ContentRequest(ticket, id);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
        assertCinnamonError(response, ErrorCode.REMOTE_SERVER_ERROR, SC_INTERNAL_SERVER_ERROR);
        TestServlet.statusCode = SC_OK;
    }

    // everything other than /getContent is an invalid request.
    @Test
    public void otherRequestsAreBad() throws IOException {
        ContentRequest contentRequest = new ContentRequest(ticket, 6L);
        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_NOTHING, contentRequest);
        assertEquals(SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }

    @Test
    public void parallelRequests() throws IOException, InterruptedException {
        BlockingArrayQueue<Runnable> runnables = new BlockingArrayQueue<>();
        ThreadPoolExecutor           executor  = new ThreadPoolExecutor(8, 16, 10, TimeUnit.SECONDS, runnables);
        TestServlet.waitForMillis = 1000L;
        final AtomicLong counter = new AtomicLong();
        TestServlet.isCurrent = false;
        for (int x = 0; x < 32; x++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        TestServlet.isCurrent = counter.incrementAndGet() % 2 == 0;
                        Long id = 9L;
                        createContentMeta(id);
                        ContentRequest contentRequest = new ContentRequest(ticket, id);
                        HttpResponse   response       = sendRequest(UrlMapping.CONTENT__GET_CONTENT, contentRequest);
                        StatusLine     statusLine     = response.getStatusLine();
                        int            statusCode     = statusLine.getStatusCode();
                        assertEquals(SC_OK, statusCode);
                    } catch (IOException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        executor.awaitTermination(4, TimeUnit.SECONDS);
        TestServlet.waitForMillis = 0;

    }

    private ContentMeta createContentMeta(Long id) throws IOException {
        Path tempDir = Files.createTempDirectory("cinnamon-content-cache-test-");
        config.getServerConfig().setDataRoot(tempDir.toFile().getAbsolutePath());
        FileSystemContentProvider contentProvider = new FileSystemContentProvider();
        FileInputStream           inputStream     = new FileInputStream("pom.xml");
        ContentMeta               contentMeta     = new ContentMeta();
        contentMeta.setName("test-content");
        contentMeta.setContentType("text/plain");
        contentMeta.setId(id);
        contentMeta.setContentHash("ignored by test servlet");
        return contentProvider.writeContentStream(contentMeta, inputStream);
    }

}
