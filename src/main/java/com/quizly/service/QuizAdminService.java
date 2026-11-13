package com.quizly.service;

import com.quizly.model.ContentStatus;
import com.quizly.model.Question;
import com.quizly.model.Quiz;
import com.quizly.repository.QuizRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Validation and authoring rules for the protected quiz planner. */
public final class QuizAdminService {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private final QuizRepository quizzes;

    public QuizAdminService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    public void saveTrack(String rawId, String rawTitle, String rawTagline, String rawDescription,
                          String rawIcon, String rawLevel, String rawMinutes, String rawAccent,
                          String rawStatus, String rawRevision, String actorId) {
        String id = required(rawId, "Track ID").toLowerCase(Locale.ROOT);
        if (!SLUG.matcher(id).matches() || id.length() > 48) {
            throw new IllegalArgumentException("Track ID must use lowercase letters, numbers, and hyphens.");
        }
        Quiz existing = quizzes.findById(id).orElse(null);
        if (existing == null && rawRevision != null && !rawRevision.isBlank()) {
            throw new IllegalStateException("This track no longer exists. Reload the planner.");
        }
        if (existing != null && (rawRevision == null || rawRevision.isBlank())) {
            throw new IllegalStateException("A track with this ID already exists. Open it from the track list to edit it.");
        }

        String title = bounded(rawTitle, "Title", 2, 80);
        String tagline = bounded(rawTagline, "Tagline", 0, 120);
        String description = bounded(rawDescription, "Description", 0, 500);
        String icon = bounded(rawIcon, "Icon", 1, 8);
        String level = bounded(rawLevel, "Level", 2, 30);
        int minutes = parseInt(rawMinutes, "Estimated minutes", 1, 180);
        String accent = required(rawAccent, "Accent colour");
        if (!accent.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Accent colour must be a six-digit hex value such as #F26430.");
        }
        ContentStatus status = parseStatus(rawStatus);
        List<Question> existingQuestions = existing == null ? List.of() : existing.getQuestions();
        Quiz updated = new Quiz(id, title, tagline, description, icon, level, minutes, accent,
                existingQuestions, status);
        if (status == ContentStatus.PUBLISHED) {
            validatePublishedBank(updated);
        }
        Long revision = existing == null ? null : parseInt(rawRevision, "Revision", 0, Integer.MAX_VALUE) * 1L;
        String action = existing == null ? "track_created"
                : existing.getStatus() != status ? "track_" + status.firestoreValue() : "track_updated";
        quizzes.save(updated, actorId, revision, action, title);
    }

    public void saveQuestion(String quizId, String rawQuestionId, String rawPrompt, String rawOptions,
                             String rawCorrectIndex, String rawDifficulty, String rawExplanation,
                             String rawStatus, String rawRevision, String actorId) {
        Quiz existing = quizzes.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Choose a valid quiz track."));
        int revisionValue = parseInt(rawRevision, "Revision", 0, Integer.MAX_VALUE);
        int correctIndex = parseInt(rawCorrectIndex, "Correct answer", 0, 5);
        int difficulty = parseInt(rawDifficulty, "Difficulty", 1, 3);
        List<String> options = parseOptions(rawOptions);
        if (correctIndex >= options.size()) {
            throw new IllegalArgumentException("Choose a correct answer that has an option.");
        }
        String questionId = rawQuestionId == null || rawQuestionId.isBlank()
                ? UUID.randomUUID().toString() : rawQuestionId.strip();
        if (questionId.length() > 80 || !questionId.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Question ID contains unsupported characters.");
        }
        String prompt = bounded(rawPrompt, "Question", 5, 600);
        String explanation = bounded(rawExplanation, "Explanation", 0, 1000);
        ContentStatus status = parseStatus(rawStatus);

        List<Question> updatedQuestions = new ArrayList<>();
        boolean found = false;
        ContentStatus previousStatus = null;
        for (Question question : existing.getQuestions()) {
            if (question.getId().equals(questionId)) {
                previousStatus = question.getStatus();
                updatedQuestions.add(new Question(questionId, prompt, options, correctIndex, explanation,
                        difficulty, status));
                found = true;
            } else {
                updatedQuestions.add(question);
            }
        }
        if (!found) {
            updatedQuestions.add(new Question(questionId, prompt, options, correctIndex, explanation,
                    difficulty, status));
        }
        ensureUniqueQuestionIds(updatedQuestions);
        Quiz updated = new Quiz(existing.getId(), existing.getTitle(), existing.getTagline(),
                existing.getDescription(), existing.getIcon(), existing.getLevel(),
                existing.getEstimatedMinutes(), existing.getAccentColor(), updatedQuestions,
                existing.getStatus(), existing.getRevision());
        if (updated.isPublished()) {
            validatePublishedBank(updated);
        }
        String action = !found ? "question_created"
                : previousStatus != status ? "question_" + status.firestoreValue() : "question_updated";
        quizzes.save(updated, actorId, (long) revisionValue, action, prompt);
    }

    public void changeQuestionStatus(String quizId, String questionId, ContentStatus status,
                                     String rawRevision, String actorId) {
        Quiz existing = quizzes.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz track not found."));
        List<Question> updatedQuestions = new ArrayList<>();
        Question changed = null;
        for (Question question : existing.getQuestions()) {
            if (question.getId().equals(questionId)) {
                changed = new Question(question.getId(), question.getPrompt(), question.getOptions(),
                        question.getCorrectOptionIndex(), question.getExplanation(), question.getDifficulty(), status);
                updatedQuestions.add(changed);
            } else {
                updatedQuestions.add(question);
            }
        }
        if (changed == null) {
            throw new IllegalArgumentException("Question not found.");
        }
        Quiz updated = new Quiz(existing.getId(), existing.getTitle(), existing.getTagline(),
                existing.getDescription(), existing.getIcon(), existing.getLevel(),
                existing.getEstimatedMinutes(), existing.getAccentColor(), updatedQuestions, existing.getStatus());
        if (updated.isPublished()) {
            validatePublishedBank(updated);
        }
        long revision = parseInt(rawRevision, "Revision", 0, Integer.MAX_VALUE);
        quizzes.save(updated, actorId, revision, "question_" + status.firestoreValue(), changed.getPrompt());
    }

    public void changeTrackStatus(String quizId, ContentStatus status, String rawRevision, String actorId) {
        Quiz existing = quizzes.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz track not found."));
        Quiz updated = new Quiz(existing.getId(), existing.getTitle(), existing.getTagline(),
                existing.getDescription(), existing.getIcon(), existing.getLevel(),
                existing.getEstimatedMinutes(), existing.getAccentColor(), existing.getQuestions(), status,
                existing.getRevision());
        if (status == ContentStatus.PUBLISHED) {
            validatePublishedBank(updated);
        }
        long revision = parseInt(rawRevision, "Revision", 0, Integer.MAX_VALUE);
        quizzes.save(updated, actorId, revision, "track_" + status.firestoreValue(), existing.getTitle());
    }

    private void validatePublishedBank(Quiz quiz) {
        long[] byDifficulty = new long[4];
        for (Question question : quiz.getQuestions()) {
            if (question.isPublished()) {
                byDifficulty[question.getDifficulty()]++;
            }
        }
        List<String> missing = new ArrayList<>();
        String[] names = {"", "Easy", "Medium", "Hard"};
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            if (byDifficulty[difficulty] < 2) {
                missing.add(names[difficulty] + " (" + byDifficulty[difficulty] + "/2 published)");
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("This track needs at least two published questions at each difficulty. Still needed: "
                    + String.join(", ", missing) + ".");
        }
    }

    private List<String> parseOptions(String rawOptions) {
        if (rawOptions == null) {
            throw new IllegalArgumentException("Enter at least two answer options, one per line.");
        }
        List<String> options = rawOptions.lines().map(String::strip).filter(value -> !value.isBlank()).toList();
        if (options.size() < 2 || options.size() > 6) {
            throw new IllegalArgumentException("A question must have between two and six answer options.");
        }
        Set<String> unique = new HashSet<>();
        for (String option : options) {
            if (option.length() > 200 || !unique.add(option.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Options must be unique and no longer than 200 characters.");
            }
        }
        return options;
    }

    private void ensureUniqueQuestionIds(List<Question> questions) {
        Set<String> ids = new HashSet<>();
        for (Question question : questions) {
            if (!ids.add(question.getId())) {
                throw new IllegalArgumentException("Question IDs must be unique within a track.");
            }
        }
    }

    private ContentStatus parseStatus(String value) {
        try {
            return ContentStatus.valueOf(required(value, "Status").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose draft, published, or archived status.");
        }
    }

    private String bounded(String raw, String label, int min, int max) {
        String value = raw == null ? "" : raw.strip();
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(label + " must be " + min + "–" + max + " characters long.");
        }
        return value;
    }

    private String required(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return raw.strip();
    }

    private int parseInt(String raw, String label, int min, int max) {
        try {
            int value = Integer.parseInt(required(raw, label));
            if (value < min || value > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }
}
