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
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.*;

@MultipartConfig
@WebServlet(name = "Osd", urlPatterns = "/")
public class ContentServlet extends HttpServlet {

    private              ObjectMapper xmlMapper = new XmlMapper();
    private static final Logger       log       = LogManager.getLogger(ContentServlet.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

    private void getContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ContentRequest contentRequest = xmlMapper.readValue(request.getInputStream(), ContentRequest.class);
        if (contentRequest.validated()) {
            // 1. available?
            FileSystemContentProvider contentProvider = new FileSystemContentProvider();
            Optional<ContentMeta>     contentMetaOpt  = contentProvider.getContentMeta(contentRequest.getId());
            RemoteConfig              remoteConfig    = CinnamonCacheServer.config.getRemoteConfig();

            if (contentMetaOpt.isPresent()) {
                String isCurrentUrl = remoteConfig.generateIsCurrentUrl();
                HttpResponse httpResponse = Request.Post(isCurrentUrl)
                        .addHeader("ticket", contentRequest.getTicket())
                        .bodyForm(Form.form().add("id", contentRequest.getId().toString()).build())
                        .execute().returnResponse();
                StatusLine statusLine = httpResponse.getStatusLine();
                int        statusCode = statusLine.getStatusCode();
                switch (statusCode) {
                    case SC_NOT_MODIFIED:
                        // get local file
                        break;
                    case SC_OK:
                        // content = remote file

                        // store content

                        // store metadata (content-type, hash)

                        break;
                    default:
                        ErrorResponseGenerator.generateErrorMessage(response, statusCode, ErrorCode.REMOTE_SERVER_ERROR, statusLine.getReasonPhrase());
                        return;
                }


            }
            else{
                // call getContent on remote server.

                // store content

                // store metadata (content-type, hash)
            }
            // send content to client

        } else {
            generateErrorMessage(response, SC_BAD_REQUEST, ErrorCode.INVALID_REQUEST);
        }
    }

    private void generateErrorMessage(HttpServletResponse response, int statusCode, ErrorCode errorCode) {
        ErrorResponseGenerator.generateErrorMessage(response, statusCode, errorCode);
    }
}
