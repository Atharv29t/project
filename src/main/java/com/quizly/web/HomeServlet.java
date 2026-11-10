package com.quizly.web;

import com.quizly.model.LeaderboardEntry;
import com.quizly.repository.QuizRepository;
import com.quizly.service.LeaderboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Landing page with the quiz catalogue and a short leaderboard preview.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"", "/home"})
public final class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);

        LeaderboardService leaderboard = WebSupport.service(request, App.LEADERBOARD_SERVICE);
        QuizRepository quizRepository = WebSupport.service(request, App.QUIZ_REPOSITORY);
        List<LeaderboardEntry> topThree = leaderboard.topEntries(null, 3);
        var quizzes = quizRepository.findPublished();

        request.setAttribute(App.ACTIVE_NAV, "home");
        request.setAttribute("quizzes", quizzes);
        request.setAttribute("quizCount", quizzes.size());
        request.setAttribute("questionCount", quizRepository.totalQuestions());
        request.setAttribute("attemptCount", leaderboard.stats(null).getAttempts());
        request.setAttribute("topPreview", topThree);

        WebSupport.forward(request, response, "home");
    }
}
