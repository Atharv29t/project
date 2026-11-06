package com.quizly.web;

import com.quizly.model.LeaderboardEntry;
import com.quizly.model.LeaderboardStats;
import com.quizly.model.Quiz;
import com.quizly.repository.QuizRepository;
import com.quizly.service.LeaderboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Top-10 leaderboard, optionally filtered by quiz track.
 */
@WebServlet(name = "LeaderboardServlet", urlPatterns = "/leaderboard")
public final class LeaderboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession(false);
        String lastResultId = session == null ? null : (String) session.getAttribute(App.LAST_RESULT_ID);

        LeaderboardService leaderboard = WebSupport.service(request, App.LEADERBOARD_SERVICE);
        QuizRepository quizRepository = WebSupport.service(request, App.QUIZ_REPOSITORY);

        String requestedQuizId = request.getParameter("quiz");
        Quiz selectedQuiz = quizRepository.findById(requestedQuizId).orElse(null);
        String selectedQuizId = selectedQuiz == null ? null : selectedQuiz.getId();

        List<LeaderboardEntry> entries = leaderboard.topEntries(selectedQuizId, 10);
        LeaderboardStats stats = leaderboard.stats(selectedQuizId);

        request.setAttribute(App.ACTIVE_NAV, "leaderboard");
        request.setAttribute("entries", entries);
        request.setAttribute("stats", stats);
        request.setAttribute("quizzes", quizRepository.findAll());
        request.setAttribute("selectedQuizId", selectedQuizId);
        request.setAttribute(App.LAST_RESULT_ID, lastResultId);

        WebSupport.forward(request, response, "leaderboard");
    }
}
