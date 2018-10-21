package com.dewarim.cinnamon.application.servlet;

import com.dewarim.cinnamon.application.ErrorCode;
import com.dewarim.cinnamon.application.ErrorResponseGenerator;
import com.dewarim.cinnamon.model.ContentMeta;
import com.dewarim.cinnamon.model.request.ContentRequest;
import com.dewarim.cinnamon.model.response.GenericResponse;
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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.entity.ContentType.APPLICATION_XML;
import static org.apache.http.entity.mime.MIME.CONTENT_DISPOSITION;

/**
 * This servlet acts like a Cinnamon 3 test server.
 * It implements the methods getContent(id) and isCurrent(id,hash)
 */
@WebServlet(name = "Test", urlPatterns = "/")
public class TestServlet extends HttpServlet {

    private static final Logger          log              = LogManager.getLogger(TestServlet.class);
    public static final  GenericResponse GENERIC_RESPONSE = new GenericResponse("Fresh content with a generic response.", true);
    public static        boolean         isCurrent        = false;
    public static        boolean         hasContent       = true;
    public static        int             statusCode       = SC_OK;
    public static        long            waitForMillis    = 0;
    public static        String          nonExistingHash  = "";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (statusCode != SC_OK) {
            response.setStatus(statusCode);
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        switch (pathInfo) {
            case "/getContent":
                getContent(request, response);
                break;
            case "/isCurrent":
                isCurrent(request, response);
                break;
            case "/exists":
                exists(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

    }

    private void exists(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contentHash = request.getParameter("contentHash");
        if (contentHash.equals(nonExistingHash)) {
            response.setStatus(SC_NO_CONTENT);
        }
        else {
            response.setStatus(SC_OK);
        }
    }

    private void isCurrent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isCurrent) {
            log.debug("Object is current");
            response.setStatus(SC_NOT_MODIFIED);
            response.setContentType(APPLICATION_XML.getMimeType());
            response.getWriter().print("<cinnamon><successful>true</successful><status>CONTENT IS CURRENT</status></cinnamon>");
        } else {
            log.debug("Object is not current");
            sendContentFile(response);
        }
    }

    private void getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (hasContent) {
            sendContentFile(response);
        } else {
            response.setStatus(SC_NOT_FOUND);
            response.setContentType(APPLICATION_XML.getMimeType());
            response.getWriter().print("<error><code>NO CONTENT FOUND</code><message/></error>");
        }
    }

    private void sendContentFile(HttpServletResponse response) throws IOException {
        waitIfRequired();
        response.setStatus(SC_OK);
        response.setContentType(APPLICATION_XML.getMimeType());
        response.setHeader(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", "GenericResponse.xml"));
        ObjectMapper mapper = new XmlMapper();
        mapper.writeValue(response.getWriter(), GENERIC_RESPONSE);
    }

    private void waitIfRequired() {
        if (waitForMillis == 0) {
            return;
        }
        try {
            Thread.sleep(waitForMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
