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
 * Account registration page. A successful sign-up logs the player in.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public final class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        if (WebSupport.currentUser(request) != null) {
            WebSupport.redirect(request, response, "/");
            return;
        }

        request.setAttribute(App.ACTIVE_NAV, "register");
        WebSupport.forward(request, response, "register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession();
        String name = request.getParameter("name");
        String email = request.getParameter("email");

        if (!WebSupport.isCsrfValid(request)) {
            WebSupport.flash(session, App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/register");
            return;
        }

        try {
            AuthService authService = WebSupport.service(request, App.AUTH_SERVICE);
            User user = authService.register(
                    name,
                    email,
                    request.getParameter("password"),
                    request.getParameter("confirmPassword")
            );

            request.changeSessionId();
            session.removeAttribute(App.ATTEMPT);
            session.removeAttribute(App.LAST_RESULT);
            session.removeAttribute(App.LAST_RESULT_ID);
            session.setAttribute(App.CURRENT_USER, user);
            session.setAttribute(App.PLAYER_NAME, user.getName());
            WebSupport.flash(session, App.FLASH_SUCCESS, "Your account is ready. Pick a track and play.");
            WebSupport.redirect(request, response, "/");
        } catch (IllegalArgumentException exception) {
            request.setAttribute(App.ACTIVE_NAV, "register");
            request.setAttribute("nameValue", name);
            request.setAttribute("emailValue", email);
            request.setAttribute("formError", exception.getMessage());
            WebSupport.forward(request, response, "register");
        }
    }
}
