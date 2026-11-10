package com.quizly.web;

import com.quizly.model.ContentStatus;
import com.quizly.model.Question;
import com.quizly.model.Quiz;
import com.quizly.model.User;
import com.quizly.repository.QuizRepository;
import com.quizly.service.QuizAdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** Admin-only quiz and question authoring screens. */
@WebServlet(name = "AdminServlet", urlPatterns = "/admin")
public final class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        QuizRepository repository = WebSupport.service(request, App.QUIZ_REPOSITORY);
        List<Quiz> quizzes = repository.findAll();
        String quizId = request.getParameter("quizId");
        Quiz selected = quizzes.stream().filter(quiz -> quiz.getId().equals(quizId)).findFirst()
                .orElseGet(() -> quizzes.isEmpty() ? null : quizzes.get(0));

        if ("new".equals(quizId)) {
            selected = new Quiz("", "", "", "", "🎯", "Beginner", 5, "#F26430", List.of(), ContentStatus.DRAFT);
        }
        Question selectedQuestion = null;
        String questionId = request.getParameter("questionId");
        if (selected != null && questionId != null) {
            selectedQuestion = selected.getQuestions().stream()
                    .filter(question -> question.getId().equals(questionId)).findFirst().orElse(null);
        }

        request.setAttribute("quizzes", quizzes);
        request.setAttribute("selectedQuiz", selected);
        request.setAttribute("selectedQuestion", selectedQuestion);
        request.setAttribute("recentAuditLogs", repository.recentAuditLogs(15));
        request.setAttribute(App.ACTIVE_NAV, "admin");
        request.setAttribute("pageTitle", "Quiz planner");
        WebSupport.forward(request, response, "admin");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        WebSupport.noCache(response);
        User actor = WebSupport.currentUser(request);
        if (!WebSupport.isCsrfValid(request)) {
            WebSupport.flash(request.getSession(), App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/admin");
            return;
        }

        String action = request.getParameter("action");
        String quizId = request.getParameter("quizId");
        QuizAdminService service = WebSupport.service(request, App.QUIZ_ADMIN_SERVICE);
        try {
            if ("saveTrack".equals(action)) {
                String rawId = request.getParameter("id");
                service.saveTrack(rawId, request.getParameter("title"), request.getParameter("tagline"),
                        request.getParameter("description"), request.getParameter("icon"),
                        request.getParameter("level"), request.getParameter("estimatedMinutes"),
                        request.getParameter("accentColor"), request.getParameter("status"),
                        request.getParameter("revision"), actor.getId());
                quizId = rawId;
                WebSupport.flash(request.getSession(), App.FLASH_SUCCESS, "Quiz track saved.");
            } else if ("saveQuestion".equals(action)) {
                service.saveQuestion(quizId, request.getParameter("questionId"), request.getParameter("prompt"),
                        request.getParameter("options"), request.getParameter("correctOptionIndex"),
                        request.getParameter("difficulty"), request.getParameter("explanation"),
                        request.getParameter("questionStatus"), request.getParameter("revision"), actor.getId());
                WebSupport.flash(request.getSession(), App.FLASH_SUCCESS, "Question saved.");
            } else if ("changeTrackStatus".equals(action)) {
                service.changeTrackStatus(quizId, status(request.getParameter("status")),
                        request.getParameter("revision"), actor.getId());
                WebSupport.flash(request.getSession(), App.FLASH_SUCCESS, "Quiz track status updated.");
            } else if ("changeQuestionStatus".equals(action)) {
                service.changeQuestionStatus(quizId, request.getParameter("questionId"),
                        status(request.getParameter("status")), request.getParameter("revision"), actor.getId());
                WebSupport.flash(request.getSession(), App.FLASH_SUCCESS, "Question status updated.");
            } else {
                throw new IllegalArgumentException("Choose a valid planner action.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            WebSupport.flash(request.getSession(), App.FLASH_ERROR, exception.getMessage());
        }
        WebSupport.redirect(request, response, "/admin?quizId=" + encode(quizId));
    }

    private ContentStatus status(String value) {
        try {
            return ContentStatus.valueOf(value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose draft, published, or archived status.");
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
