package com.quizly.web;

import com.quizly.model.User;
import com.quizly.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Rechecks the account's database role for every admin request. */
@WebFilter(filterName = "AdminAuthorizationFilter", urlPatterns = {"/admin", "/admin/*"})
public final class AdminAuthorizationFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                         jakarta.servlet.ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        User sessionUser = WebSupport.currentUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?next=%2Fadmin");
            return;
        }

        UserRepository users = WebSupport.service(request, App.USER_REPOSITORY);
        User currentUser = users.findById(sessionUser.getId()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        request.getSession().setAttribute(App.CURRENT_USER, currentUser);
        chain.doFilter(request, response);
    }
}
