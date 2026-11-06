package com.quizly.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Ends the current session. Logout is POST-only so it can be CSRF protected.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public final class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && WebSupport.isCsrfValid(request)) {
            session.invalidate();
            HttpSession freshSession = request.getSession(true);
            WebSupport.flash(freshSession, App.FLASH_SUCCESS, "You have been logged out.");
        }
        WebSupport.redirect(request, response, "/");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WebSupport.redirect(request, response, "/");
    }
}
