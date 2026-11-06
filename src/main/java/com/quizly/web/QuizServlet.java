package com.quizly.web;

import com.quizly.model.Attempt;
import com.quizly.model.Result;
import com.quizly.repository.ResultRepository;
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

        boolean skipped = request.getParameter("skip") != null;
        try {
            attempt.answerCurrent(
                    request.getParameter("questionId"),
                    skipped ? null : request.getParameter("answer")
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            WebSupport.flash(session, App.FLASH_ERROR, exception.getMessage());
            WebSupport.redirect(request, response, "/quiz");
            return;
        }

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

        session.removeAttribute(App.ATTEMPT);
        session.setAttribute(App.LAST_RESULT, result);
        session.setAttribute(App.LAST_RESULT_ID, result.getId());
        return new GradeResult(result);
    }

    /** Named result record just so the code tells the reader what it produced. */
    private record GradeResult(Result result) {
    }
}
