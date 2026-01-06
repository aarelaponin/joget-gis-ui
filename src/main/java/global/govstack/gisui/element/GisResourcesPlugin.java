package global.govstack.gisui.element;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.plugin.base.PluginWebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * GIS Resources Plugin
 *
 * Serves static files (JavaScript, CSS) for the GIS Polygon Capture component.
 * Uses Joget's PluginWebSupport interface to provide a web endpoint.
 *
 * Access via: /jw/web/json/plugin/global.govstack.gisui.element.GisResourcesPlugin/service?file=<filename>
 */
public class GisResourcesPlugin extends ExtDefaultPlugin implements PluginWebSupport {

    private static final String CLASS_NAME = GisResourcesPlugin.class.getName();

    @Override
    public String getName() {
        return "GIS Resources Provider";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Serves static resources (JS, CSS) for GIS Polygon Capture component";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String file = request.getParameter("file");

        if (file == null || file.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
            return;
        }

        // Security: prevent directory traversal
        if (file.contains("..") || file.contains("/") || file.contains("\\")) {
            LogUtil.warn(CLASS_NAME, "Blocked potential directory traversal: " + file);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
            return;
        }

        // Determine content type
        String contentType = getContentType(file);
        response.setContentType(contentType);

        // Set caching headers (1 year for versioned resources)
        String version = request.getParameter("v");
        if (version != null && !version.isEmpty()) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else {
            response.setHeader("Cache-Control", "public, max-age=3600");
        }

        // Load from classpath
        String resourcePath = "/static/" + file;
        try {
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl == null) {
                LogUtil.warn(CLASS_NAME, "Resource not found: " + resourcePath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + file);
                return;
            }

            try (InputStream in = resourceUrl.openStream();
                 OutputStream out = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }

        } catch (Exception e) {
            LogUtil.error(CLASS_NAME, e, "Error serving resource: " + file);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading resource");
        }
    }

    /**
     * Get MIME content type based on file extension.
     */
    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (lower.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lower.endsWith(".woff")) {
            return "font/woff";
        } else if (lower.endsWith(".woff2")) {
            return "font/woff2";
        } else if (lower.endsWith(".ttf")) {
            return "font/ttf";
        } else if (lower.endsWith(".eot")) {
            return "application/vnd.ms-fontobject";
        } else {
            return "application/octet-stream";
        }
    }
}
