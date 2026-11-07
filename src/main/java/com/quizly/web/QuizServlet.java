package com.quizly.web;

import com.quizly.model.Attempt;
import com.quizly.model.Result;
import com.quizly.repository.ResultRepository;
import com.quizly.repository.ProgressionRepository;
import com.quizly.service.RoomService;
import com.quizly.service.QuizService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Displays one question per request and records each answer in the session.
 */
@WebServlet(name = "QuizServlet", urlPatterns = "/quiz")
public final class QuizServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession(false);

        Attempt attempt = session == null ? null : (Attempt) session.getAttribute(App.ATTEMPT);
        if (attempt == null) {
            WebSupport.flash(session, App.FLASH_ERROR, "Start a quiz before viewing a question.");
            WebSupport.redirect(request, response, "/");
            return;
        }

        if (attempt.isComplete()) {
            GradeResult grade = gradeAndStore(request, session, attempt);
            WebSupport.redirect(request, response, "/result");
            return;
        }

        request.setAttribute(App.ACTIVE_NAV, "quiz");
        request.setAttribute(App.ATTEMPT, attempt);
        WebSupport.forward(request, response, "quiz");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        HttpSession session = request.getSession(false);

        Attempt attempt = session == null ? null : (Attempt) session.getAttribute(App.ATTEMPT);
        if (session == null || attempt == null) {
            WebSupport.redirect(request, response, "/");
            return;
        }

        if (!WebSupport.isCsrfValid(request)) {
            WebSupport.flash(session, App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/quiz");
            return;
        }

        String powerUp = request.getParameter("powerUp");
        if (powerUp != null) {
            try {
                attempt.usePowerUp(powerUp);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                WebSupport.flash(session, App.FLASH_ERROR, exception.getMessage());
            }
            updateRoomProgress(request, session, attempt);
            if (attempt.isComplete()) {
                gradeAndStore(request, session, attempt);
                WebSupport.redirect(request, response, "/result");
            } else {
                WebSupport.redirect(request, response, "/quiz");
            }
            return;
        }

        try {
            attempt.answerCurrent(
                    request.getParameter("questionId"),
                    request.getParameter("timeout") == null ? request.getParameter("answer") : null
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            WebSupport.flash(session, App.FLASH_ERROR, exception.getMessage());
            WebSupport.redirect(request, response, "/quiz");
            return;
        }

        updateRoomProgress(request, session, attempt);
        if (attempt.isComplete()) {
            gradeAndStore(request, session, attempt);
            WebSupport.redirect(request, response, "/result");
        } else {
            WebSupport.redirect(request, response, "/quiz");
        }
    }

    /**
     * Grades the attempt, stores the summary and prepares the result page.
     */
    private GradeResult gradeAndStore(HttpServletRequest request, HttpSession session, Attempt attempt) {
        QuizService quizService = WebSupport.service(request, App.QUIZ_SERVICE);
        ResultRepository repository = WebSupport.service(request, App.RESULT_REPOSITORY);

        Result result = quizService.grade(attempt);
        repository.save(result);
        if (attempt.getUserId() != null) {
            ProgressionRepository progression = WebSupport.service(request, App.PROGRESSION_REPOSITORY);
            var updatedProgression = progression.recordQuizCompletion(
                    attempt.getUserId(), attempt.getId(), result.getScore(), result.getBonusPoints() > 0);
            session.setAttribute(App.LAST_XP_AWARD, updatedProgression.getLastAward());
        }

        session.removeAttribute(App.ATTEMPT);
        Object roomCode = session.getAttribute(App.ROOM_CODE);
        if (roomCode instanceof String code && attempt.getUserId() != null) {
            session.setAttribute(App.LAST_ROOM_CODE, code);
            RoomService rooms = WebSupport.service(request, App.ROOM_SERVICE);
            try {
                rooms.completeAttempt(code, attempt.getUserId(), result);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                WebSupport.flash(session, App.FLASH_ERROR,
                        "The live room ended before your result was posted. Your result is saved.");
            }
        }
        session.removeAttribute(App.ROOM_CODE);
        session.setAttribute(App.LAST_RESULT, result);
        session.setAttribute(App.LAST_RESULT_ID, result.getId());
        return new GradeResult(result);
    }

    private void updateRoomProgress(HttpServletRequest request, HttpSession session, Attempt attempt) {
        Object roomCode = session.getAttribute(App.ROOM_CODE);
        if (roomCode instanceof String code && attempt.getUserId() != null) {
            RoomService rooms = WebSupport.service(request, App.ROOM_SERVICE);
            try {
                rooms.recordProgress(code, attempt.getUserId(), attempt.getCurrentIndex());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                session.removeAttribute(App.ROOM_CODE);
                WebSupport.flash(session, App.FLASH_ERROR,
                        "The live room ended. Your quiz can continue and your result will still be saved.");
            }
        }
    }

    /** Named result record just so the code tells the reader what it produced. */
    private record GradeResult(Result result) {
    }
}
