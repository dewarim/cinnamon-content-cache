package com.dewarim.cinnamon.test.integration;

import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.UrlMapping;
import com.dewarim.cinnamon.application.servlet.TestServlet;
import com.dewarim.cinnamon.model.response.CinnamonError;
import com.dewarim.cinnamon.model.response.GenericResponse;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class CinnamonIntegrationTest {
    
    static int                 cinnamonTestPort = 19999;
    static CinnamonCacheServer cinnamonCacheServer;
    static String              ticket = "test";
    static String              HOST = "http://localhost:"+cinnamonTestPort;
    XmlMapper mapper = new XmlMapper();
    
    @BeforeClass
    public static void setUpServer() throws Exception{
        if(cinnamonCacheServer == null) {
            cinnamonCacheServer = new CinnamonCacheServer(cinnamonTestPort);
            cinnamonCacheServer.getWebAppContext().addServlet(TestServlet.class, "/test/*");
            cinnamonCacheServer.start();

            // set data root:
            Path tempDirectory = Files.createTempDirectory("cinnamon-data-root");
            CinnamonCacheServer.config.getServerConfig().setDataRoot(tempDirectory.toAbsolutePath().toString());
        }
    }


    protected void assertResponseOkay(HttpResponse response){
        assertThat(response.getStatusLine().getStatusCode(),equalTo(HttpStatus.SC_OK));
    }
    
    protected void assertCinnamonError(HttpResponse response, ErrorCode errorCode) throws IOException{
        Assert.assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_BAD_REQUEST));
        CinnamonError cinnamonError = mapper.readValue(response.getEntity().getContent(), CinnamonError.class);
        Assert.assertThat(cinnamonError.getCode(), equalTo(errorCode.getCode()));  
    }  
    
    protected void assertCinnamonError(HttpResponse response, ErrorCode errorCode, int statusCode ) throws IOException{
        Assert.assertThat(response.getStatusLine().getStatusCode(), equalTo(statusCode));
        CinnamonError cinnamonError = mapper.readValue(response.getEntity().getContent(), CinnamonError.class);
        Assert.assertThat(cinnamonError.getCode(), equalTo(errorCode.getCode()));  
    }

    /**
     * Send a POST request with the admin's ticket to the Cinnamon server. 
     * The request object will be serialized and put into the
     * request body.
     * @param urlMapping defines the API method you want to call
     * @param request request object to be sent to the server as XML string.
     * @return the server's response.
     * @throws IOException if connection to server fails for some reason
     */
    protected HttpResponse sendRequest(UrlMapping urlMapping, Object request) throws IOException {
        String requestStr = mapper.writeValueAsString(request);
        return Request.Post("http://localhost:" + cinnamonTestPort + urlMapping.getPath())
                .addHeader("ticket", ticket)
                .bodyString(requestStr, ContentType.APPLICATION_XML)
                .execute().returnResponse();
    }
    
    protected GenericResponse parseGenericResponse(HttpResponse response) throws IOException{
        assertResponseOkay(response);
        return mapper.readValue(response.getEntity().getContent(),GenericResponse.class);
    }
}
