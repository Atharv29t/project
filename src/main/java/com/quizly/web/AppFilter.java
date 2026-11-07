package com.quizly.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Cross-cutting filter.
 *
 * <ul>
 *   <li>sets UTF-8 for every request/response</li>
 *   <li>creates one CSRF token per browser session</li>
 *   <li>moves one-shot flash messages from session scope to request scope</li>
 *   <li>adds basic browser security headers</li>
 * </ul>
 */
@jakarta.servlet.annotation.WebFilter(filterName = "AppFilter", urlPatterns = "/*")
public final class AppFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                         jakarta.servlet.ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; script-src 'self'; connect-src 'self' ws: wss:; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isStaticAsset = path.startsWith("/assets/") || path.startsWith("/favicon");

        if (!isStaticAsset) {
            HttpSession session = request.getSession();
            WebSupport.ensureCsrf(session);

            // One-shot flash messages: display them on the next page only.
            String success = (String) session.getAttribute(App.FLASH_SUCCESS);
            String error = (String) session.getAttribute(App.FLASH_ERROR);
            if (success != null) {
                request.setAttribute(App.FLASH_SUCCESS, success);
                session.removeAttribute(App.FLASH_SUCCESS);
            }
            if (error != null) {
                request.setAttribute(App.FLASH_ERROR, error);
                session.removeAttribute(App.FLASH_ERROR);
            }
        }

        chain.doFilter(request, response);
    }
}
