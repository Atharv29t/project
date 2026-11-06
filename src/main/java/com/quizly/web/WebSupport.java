package com.quizly.web;

import com.quizly.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Small helpers shared by every servlet.
 */
public final class WebSupport {

    private static final SecureRandom RANDOM = new SecureRandom();

    private WebSupport() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T service(HttpServletRequest request, String name) {
        Object value = request.getServletContext().getAttribute(name);
        if (value == null) {
            throw new IllegalStateException("Application attribute '" + name + "' is not initialised");
        }
        return (T) value;
    }

    /**
     * Adds a message to the next request (usually after a redirect).
     */
    public static void flash(HttpSession session, String attributeName, String message) {
        session.setAttribute(attributeName, message);
    }

    /**
     * Forwards to a JSP kept safely under <code>/WEB-INF</code>.
     */
    public static void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    public static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * CSRF guard for all POST forms. The token is randomly created once per
     * session by <code>AppFilter</code>.
     */
    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(App.CURRENT_USER);
    }

    public static String safeRedirectPath(String path, String fallback) {
        if (path == null || path.isBlank()) {
            return fallback;
        }
        String candidate = path.strip();
        if (!candidate.startsWith("/") || candidate.startsWith("//")
                || candidate.contains("://") || candidate.contains("\\")) {
            return fallback;
        }
        return candidate;
    }

    public static void ensureCsrf(HttpSession session) {
        if (session.getAttribute(App.CSRF_TOKEN) == null) {
            byte[] tokenBytes = new byte[32];
            RANDOM.nextBytes(tokenBytes);
            session.setAttribute(App.CSRF_TOKEN, Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes));
        }
    }

    public static boolean isCsrfValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(App.CSRF_TOKEN);
        String supplied = request.getParameter("csrfToken");
        return expected != null && supplied != null && Objects.equals(expected.toString(), supplied.strip());
    }

    /**
     * Stops browsers from storing dynamic pages in their cache. Important when
     * an in-progress quiz or result should disappear after a reload/back action.
     */
    public static void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
    }
}
