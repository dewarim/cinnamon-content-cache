package com.dewarim.cinnamon.application.servlet;

import com.dewarim.cinnamon.application.CinnamonCacheServer;
import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.ErrorResponseGenerator;
import com.dewarim.cinnamon.configuration.RemoteConfig;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.model.request.*;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.entity.mime.MIME.CONTENT_DISPOSITION;

@WebServlet(name = "Content", urlPatterns = "/")
public class ContentServlet extends HttpServlet {

    private              ObjectMapper xmlMapper = new XmlMapper();
    private static final Logger       log       = LogManager.getLogger(ContentServlet.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        switch (pathInfo) {
            case "/getContent":
                getContent(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

    }

    private void getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentRequest contentRequest = xmlMapper.readValue(request.getInputStream(), ContentRequest.class);
        if (contentRequest.validated()) {
            FileSystemContentProvider contentProvider = new FileSystemContentProvider();
            Optional<ContentMeta>     contentMetaOpt  = contentProvider.getContentMeta(contentRequest.getId());
            RemoteConfig              remoteConfig    = CinnamonCacheServer.config.getRemoteConfig();

            InputStream contentStream = null;
            Long        id            = contentRequest.getId();
            String      ticket        = contentRequest.getTicket();
            ContentMeta meta          = null;
            try {
                if (contentMetaOpt.isPresent()) {
                    meta = contentMetaOpt.get();
                    String isCurrentUrl = remoteConfig.generateIsCurrentUrl();
                    HttpResponse httpResponse = Request.Post(isCurrentUrl)
                            .addHeader("ticket", ticket)
                            .bodyForm(Form.form().add("id", id.toString()).build())
                            .execute().returnResponse();
                    StatusLine statusLine = httpResponse.getStatusLine();
                    int        statusCode = statusLine.getStatusCode();
                    switch (statusCode) {
                        case SC_NOT_MODIFIED:
                            // get local file
                            contentStream = contentProvider.getContentStream(meta);
                            break;
                        case SC_OK:
                            // response content is current remote file
                            meta = getMetaFromResponse(id, httpResponse);
                            meta = contentProvider.writeContentStream(meta, httpResponse.getEntity().getContent());
                            contentStream = contentProvider.getContentStream(meta);
                            break;
                        default:
                            ErrorResponseGenerator.generateErrorMessage(response, statusCode, ErrorCode.REMOTE_SERVER_ERROR, statusLine.getReasonPhrase());
                            return;
                    }
                } else {
                    // call getContent on remote server
                    HttpResponse httpResponse = fetchRemoteFile(id, ticket);
                    meta = getMetaFromResponse(id, httpResponse);
                    meta = contentProvider.writeContentStream(meta, httpResponse.getEntity().getContent());
                    contentStream = contentProvider.getContentStream(meta);
                }
                // send content to client
                if (contentStream != null) {
                    response.setStatus(SC_OK);
                    response.setContentType(meta.getContentType());
                    response.setHeader(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", meta.getName()));
                    IOUtils.copy(contentStream, response.getOutputStream());
                } else {
                    ErrorResponseGenerator.generateErrorMessage(response, SC_NOT_FOUND, ErrorCode.OBJECT_NOT_FOUND, "No error, but also: no content was found for given object.");
                }
            } catch (IOException e) {
                log.info("Failed to fetch content: ", e);
                ErrorResponseGenerator.generateErrorMessage(response, SC_INTERNAL_SERVER_ERROR, ErrorCode.IO_EXCEPTION, e.getMessage());
            }
        } else {
            ErrorResponseGenerator.generateErrorMessage(response, SC_BAD_REQUEST, ErrorCode.INVALID_REQUEST);
        }
    }

    private ContentMeta getMetaFromResponse(Long id, HttpResponse response) {
        String      disposition = response.getFirstHeader(CONTENT_DISPOSITION).getValue();
        String      filename    = disposition.replaceAll("attachment;\\s*filename=\"(^[\"]+)", "$1");
        String      contentType = response.getFirstHeader("Content-Type").getValue();
        ContentMeta contentMeta = new ContentMeta();
        contentMeta.setId(id);
        contentMeta.setContentType(contentType);
        contentMeta.setName(filename);
        return contentMeta;
    }

    private HttpResponse fetchRemoteFile(Long id, String ticket) throws IOException {
        RemoteConfig remoteConfig  = CinnamonCacheServer.config.getRemoteConfig();
        String       getContentUrl = remoteConfig.generateGetContentUrl();
        HttpResponse httpResponse = Request.Post(getContentUrl)
                .addHeader("ticket", ticket)
                .bodyForm(Form.form().add("id", id.toString()).build())
                .execute().returnResponse();
        StatusLine statusLine = httpResponse.getStatusLine();
        int        statusCode = statusLine.getStatusCode();
        log.debug("Response for fetchRemoteFile for #{}: {} ", id, statusLine);
        switch (statusCode) {
            case SC_OK:
                return httpResponse;
            default:
                throw new IOException("Could not fetch remote file.");
        }
    }

}
