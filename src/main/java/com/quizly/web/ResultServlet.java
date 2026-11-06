package com.quizly.web;

import com.quizly.model.Result;
import com.quizly.model.User;
import com.quizly.repository.ResultRepository;
import com.quizly.service.LeaderboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Shows a graded result. The result may be the one just completed in this
 * session, or a saved result requested with <code>?id=...</code>.
 */
@WebServlet(name = "ResultServlet", urlPatterns = "/result")
public final class ResultServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession(false);
        User user = WebSupport.currentUser(request);

        ResultRepository repository = WebSupport.service(request, App.RESULT_REPOSITORY);
        Result result;
        String requestedId = request.getParameter("id");
        if (requestedId != null && !requestedId.isBlank()) {
            result = repository.findById(requestedId).orElse(null);
            if (result != null && result.getUserId() != null
                    && (user == null || !result.getUserId().equals(user.getId()))) {
                WebSupport.flash(session, App.FLASH_ERROR, "That result belongs to another player.");
                WebSupport.redirect(request, response, "/leaderboard");
                return;
            }
        } else {
            result = session == null ? null : (Result) session.getAttribute(App.LAST_RESULT);
        }

        if (result == null) {
            WebSupport.flash(session, App.FLASH_ERROR, "Take a quiz to see your result.");
            WebSupport.redirect(request, response, "/");
            return;
        }

        LeaderboardService leaderboard = WebSupport.service(request, App.LEADERBOARD_SERVICE);
        int rank = leaderboard.rankOf(result.getId(), result.getQuizId());

        request.setAttribute(App.ACTIVE_NAV, "result");
        request.setAttribute("result", result);
        request.setAttribute("leaderboardRank", rank);
        WebSupport.forward(request, response, "result");
    }
}
