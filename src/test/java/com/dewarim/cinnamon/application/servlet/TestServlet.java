package com.dewarim.cinnamon.application.servlet;

import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.ErrorResponseGenerator;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.model.request.ContentRequest;
import com.dewarim.cinnamon.provider.FileSystemContentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * This servlet acts like a Cinnamon 3 test server.
 * It implements the methods getContent(id) and isCurrent(id,hash)
 */
@WebServlet(name = "Test", urlPatterns = "/")
public class TestServlet extends HttpServlet {

    private              ObjectMapper         xmlMapper            = new XmlMapper();
    private static final Logger               log                  = LogManager.getLogger(TestServlet.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        switch (pathInfo) {
            case "/getContent":
                getContent(request, response);
                break;
            case "/isCurrent":
                isCurrent(request,response);
            default:
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

    }

    private void isCurrent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
    private void getContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ContentRequest contentRequest = xmlMapper.readValue(request.getInputStream(), ContentRequest.class);
        if (contentRequest.validated()) {
            // 1. available?
            FileSystemContentProvider contentProvider = new FileSystemContentProvider();
            Optional<ContentMeta>     contentMetaOpt     = contentProvider.getContentMeta(contentRequest.getId());
            if(contentMetaOpt.isPresent()){
                // check with main server
            }
            else{
                // get directly from main server
            }

//            contentProvider.getContentStream()

            // 2. request from main server with hash

            // 3. a) store new file



//            InputStream     contentStream   = contentProvider.getContentStream(osd);
//            response.setContentType(format.getContentType());
            response.setStatus(SC_OK);
//            contentStream.transferTo(response.getOutputStream());
        } else {
            generateErrorMessage(response, SC_BAD_REQUEST, ErrorCode.INVALID_REQUEST);
        }
    }

    private void generateErrorMessage(HttpServletResponse response, int statusCode, ErrorCode errorCode) {
        ErrorResponseGenerator.generateErrorMessage(response, statusCode, errorCode);
    }
}
