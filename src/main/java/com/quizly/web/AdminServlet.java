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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, String> formValues = takeFailedForm(request, quizId);
        String formKind = (String) request.getAttribute("adminFormKind");
        if (selected != null) {
            int[] published = publishedQuestionCounts(selected);
            request.setAttribute("publishedEasyCount", published[0]);
            request.setAttribute("publishedMediumCount", published[1]);
            request.setAttribute("publishedHardCount", published[2]);
        }

        request.setAttribute("quizzes", quizzes);
        request.setAttribute("selectedQuiz", selected);
        request.setAttribute("selectedQuestion", selectedQuestion);
        request.setAttribute("adminFormValues", formValues == null ? Map.of() : formValues);
        request.setAttribute("adminFormKind", formKind == null ? "" : formKind);
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
        String redirectPath = "/admin?quizId=" + encode(quizId);
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
                redirectPath = "/admin?quizId=" + encode(quizId) + "&newQuestion=1#questionEditor";
            } else if ("saveQuestion".equals(action)) {
                service.saveQuestion(quizId, request.getParameter("questionId"), request.getParameter("prompt"),
                        request.getParameter("options"), request.getParameter("correctOptionIndex"),
                        request.getParameter("difficulty"), request.getParameter("explanation"),
                        request.getParameter("questionStatus"), request.getParameter("revision"), actor.getId());
                WebSupport.flash(request.getSession(), App.FLASH_SUCCESS, "Question saved.");
                redirectPath = "/admin?quizId=" + encode(quizId);
                if ("addAnother".equals(request.getParameter("saveMode"))) {
                    redirectPath += "&newQuestion=1#questionEditor";
                }
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
            rememberFailedForm(request, action, quizId);
            redirectPath = "/admin?quizId=" + encode(quizId);
            if ("saveQuestion".equals(action)) {
                String questionId = request.getParameter("questionId");
                redirectPath += questionId == null || questionId.isBlank()
                        ? "&newQuestion=1#questionEditor"
                        : "&questionId=" + encode(questionId) + "#questionEditor";
            }
        }
        WebSupport.redirect(request, response, redirectPath);
    }

    private Map<String, String> takeFailedForm(HttpServletRequest request, String quizId) {
        var session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object formQuizId = session.getAttribute("adminFormQuizId");
        Object values = session.getAttribute("adminFormValues");
        Object kind = session.getAttribute("adminFormKind");
        session.removeAttribute("adminFormQuizId");
        session.removeAttribute("adminFormValues");
        session.removeAttribute("adminFormKind");
        if (!java.util.Objects.equals(quizId, formQuizId)) {
            return null;
        }
        request.setAttribute("adminFormKind", kind instanceof String text ? text : "");
        if (!(values instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, String> formValues = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key instanceof String name && value instanceof String text) {
                formValues.put(name, text);
            }
        });
        return formValues;
    }

    private void rememberFailedForm(HttpServletRequest request, String action, String quizId) {
        String kind;
        String[] fields;
        if ("saveTrack".equals(action)) {
            kind = "track";
            fields = new String[]{"id", "title", "tagline", "description", "icon", "level",
                    "estimatedMinutes", "accentColor", "status", "revision"};
        } else if ("saveQuestion".equals(action)) {
            kind = "question";
            fields = new String[]{"questionId", "prompt", "options", "correctOptionIndex", "difficulty",
                    "explanation", "questionStatus", "revision"};
        } else {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : fields) {
            values.put(field, request.getParameter(field) == null ? "" : request.getParameter(field));
        }
        var session = request.getSession();
        session.setAttribute("adminFormQuizId", quizId);
        session.setAttribute("adminFormKind", kind);
        session.setAttribute("adminFormValues", values);
    }

    private int[] publishedQuestionCounts(Quiz quiz) {
        int[] counts = new int[3];
        quiz.getQuestions().stream().filter(Question::isPublished).forEach(question -> {
            int difficulty = question.getDifficulty();
            if (difficulty >= 1 && difficulty <= 3) {
                counts[difficulty - 1]++;
            }
        });
        return counts;
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
