package com.quizly.web;

import com.quizly.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Protects pages that need an account, redirecting anonymous visitors to the
 * login page with a safe return path.
 */
@WebFilter(filterName = "AuthenticationFilter",
        urlPatterns = {"/quiz", "/quiz/start", "/result", "/profile", "/room", "/room/*"})
public final class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute(App.CURRENT_USER);

        if (user == null) {
            WebSupport.flash(session, App.FLASH_ERROR, "Please log in to play and save your scores.");
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                path += "?" + request.getQueryString();
            }
            String next = URLEncoder.encode(path, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/login?next=" + next);
            return;
        }

        chain.doFilter(request, response);
    }
}
