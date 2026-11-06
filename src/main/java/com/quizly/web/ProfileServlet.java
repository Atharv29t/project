package com.quizly.web;

import com.quizly.model.Result;
import com.quizly.model.User;
import com.quizly.repository.ResultRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * The signed-in player's account page and score history.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = "/profile")
public final class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        User user = WebSupport.currentUser(request);
        if (user == null) {
            WebSupport.redirect(request, response, "/login");
            return;
        }

        ResultRepository resultRepository = WebSupport.service(request, App.RESULT_REPOSITORY);
        List<Result> results = resultRepository.findByUser(user.getId());

        int best = results.stream().mapToInt(Result::getPercentage).max().orElse(0);
        int average = (int) Math.round(results.stream()
                .mapToInt(Result::getPercentage)
                .average()
                .orElse(0));

        request.setAttribute(App.ACTIVE_NAV, "profile");
        request.setAttribute("results", results);
        request.setAttribute("attemptCount", results.size());
        request.setAttribute("bestPercentage", best);
        request.setAttribute("averagePercentage", average);
        WebSupport.forward(request, response, "profile");
    }
}
