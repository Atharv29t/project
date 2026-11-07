package com.quizly.web;

import com.quizly.model.Attempt;
import com.quizly.model.User;
import com.quizly.service.QuizService;
import com.quizly.service.RoomService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Starts or restarts a quiz attempt for the selected track.
 */
@WebServlet(name = "StartQuizServlet", urlPatterns = "/quiz/start")
public final class StartQuizServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Quiz runs are always started by POST, so direct GET just shows the home page.
        WebSupport.redirect(request, response, "/");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession();

        if (!WebSupport.isCsrfValid(request)) {
            WebSupport.flash(session, App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/");
            return;
        }

        User user = (User) session.getAttribute(App.CURRENT_USER);
        if (user == null) {
            WebSupport.flash(session, App.FLASH_ERROR, "Please log in before starting a quiz.");
            WebSupport.redirect(request, response, "/login");
            return;
        }

        try {
            QuizService quizService = WebSupport.service(request, App.QUIZ_SERVICE);
            String roomCode = request.getParameter("roomCode");
            if (roomCode != null && !roomCode.isBlank()) {
                RoomService rooms = WebSupport.service(request, App.ROOM_SERVICE);
                var room = rooms.find(roomCode.strip().toUpperCase(java.util.Locale.ROOT), user.getId());
                if (!room.isStarted() || !room.getQuizId().equals(request.getParameter("quizId"))) {
                    throw new IllegalStateException("That room is not ready to start this quiz.");
                }
                session.setAttribute(App.ROOM_CODE, room.getCode());
            } else {
                session.removeAttribute(App.ROOM_CODE);
            }
            Attempt attempt = quizService.startAttemptForUser(user, request.getParameter("quizId"));

            session.setAttribute(App.ATTEMPT, attempt);
            session.setAttribute(App.PLAYER_NAME, attempt.getPlayerName());
            session.removeAttribute(App.LAST_RESULT);
            session.removeAttribute(App.LAST_RESULT_ID);
            session.removeAttribute(App.LAST_XP_AWARD);
            session.removeAttribute(App.LAST_ROOM_CODE);
            WebSupport.redirect(request, response, "/quiz");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            session.removeAttribute(App.ROOM_CODE);
            WebSupport.flash(session, App.FLASH_ERROR, exception.getMessage());
            WebSupport.redirect(request, response, "/");
        }
    }
}
