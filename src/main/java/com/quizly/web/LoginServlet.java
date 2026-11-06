package com.quizly.web;

import com.quizly.model.User;
import com.quizly.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Email/password login page.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public final class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        String next = WebSupport.safeRedirectPath(request.getParameter("next"), "/");
        if (WebSupport.currentUser(request) != null) {
            WebSupport.redirect(request, response, next);
            return;
        }

        request.setAttribute(App.ACTIVE_NAV, "login");
        request.setAttribute("next", next);
        WebSupport.forward(request, response, "login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession();
        String next = WebSupport.safeRedirectPath(request.getParameter("next"), "/");

        if (!WebSupport.isCsrfValid(request)) {
            WebSupport.flash(session, App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/login");
            return;
        }

        String email = request.getParameter("email");
        try {
            AuthService authService = WebSupport.service(request, App.AUTH_SERVICE);
            User user = authService.login(email, request.getParameter("password"));

            request.changeSessionId();
            session.removeAttribute(App.ATTEMPT);
            session.removeAttribute(App.LAST_RESULT);
            session.removeAttribute(App.LAST_RESULT_ID);
            session.setAttribute(App.CURRENT_USER, user);
            session.setAttribute(App.PLAYER_NAME, user.getName());
            WebSupport.flash(session, App.FLASH_SUCCESS, "Welcome back, " + user.getName() + ".");
            WebSupport.redirect(request, response, next);
        } catch (IllegalArgumentException exception) {
            request.setAttribute(App.ACTIVE_NAV, "login");
            request.setAttribute("next", next);
            request.setAttribute("emailValue", email);
            request.setAttribute("formError", exception.getMessage());
            WebSupport.forward(request, response, "login");
        }
    }
}
