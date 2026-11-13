package com.quizly.repository;

import com.quizly.data.QuizCatalog;
import com.quizly.db.Database;
import com.quizly.db.DocumentStore;
import com.quizly.model.ContentStatus;
import com.quizly.model.Question;
import com.quizly.model.Quiz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Firestore-backed quiz catalogue and admin content persistence. */
public final class QuizRepository {

    private static final long CACHE_TTL_MILLIS = 15_000;
    private static final String QUIZZES = "quizzes";
    private static final String AUDIT_LOGS = "adminAuditLogs";

    private final DocumentStore store;
    private volatile List<Quiz> cachedQuizzes;
    private volatile long cachedAtMillis;

    public QuizRepository(Database database) {
        this.store = database.store();
    }

    /** Includes all states for admin use. Cache expires so other app instances see edits. */
    public List<Quiz> findAll() {
        List<Quiz> quizzes = cachedQuizzes;
        if (quizzes != null && System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS) {
            return quizzes;
        }
        synchronized (this) {
            if (cachedQuizzes == null || System.currentTimeMillis() - cachedAtMillis >= CACHE_TTL_MILLIS) {
                cachedQuizzes = store.findAll(QUIZZES).stream()
                        .sorted(Comparator.comparingInt((Map<String, Object> document) -> number(document.get("sortOrder"), 0))
                                .thenComparing(document -> (String) document.getOrDefault("title", "")))
                        .map(this::mapQuiz)
                        .toList();
                cachedAtMillis = System.currentTimeMillis();
            }
            return cachedQuizzes;
        }
    }

    /** Published tracks and questions only; intended for player-facing flows. */
    public List<Quiz> findPublished() {
        return findAll().stream()
                .filter(Quiz::isPublished)
                .map(this::publishedQuestionsOnly)
                .toList();
    }

    public Optional<Quiz> findById(String quizId) {
        if (quizId == null || quizId.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream().filter(quiz -> quiz.getId().equals(quizId)).findFirst();
    }

    public Optional<Quiz> findPublishedById(String quizId) {
        return findById(quizId).filter(Quiz::isPublished).map(this::publishedQuestionsOnly);
    }

    public int totalQuestions() {
        return findPublished().stream().mapToInt(quiz -> quiz.getQuestions().size()).sum();
    }

    /** Writes the track and a concise audit entry atomically. */
    public void save(Quiz quiz, String actorId, Long expectedRevision) {
        save(quiz, actorId, expectedRevision, null, quiz.getTitle());
    }

    public void save(Quiz quiz, String actorId, Long expectedRevision, String action, String summary) {
        Instant now = Instant.now();
        String auditId = UUID.randomUUID().toString();
        store.transact(transaction -> {
            Map<String, Object> old = transaction.find(QUIZZES, quiz.getId());
            if (old != null && expectedRevision == null) {
                throw new IllegalStateException("A quiz track with that ID already exists.");
            }
            if (old == null && expectedRevision != null) {
                throw new IllegalStateException("This quiz changed while you were editing it. Reload and try again.");
            }
            int oldRevision = old == null ? 0 : number(old.get("revision"), 0);
            if (old != null && expectedRevision != null && oldRevision != expectedRevision) {
                throw new IllegalStateException("This quiz changed while you were editing it. Reload and try again.");
            }

            Map<String, Object> document = old == null ? new LinkedHashMap<>() : new LinkedHashMap<>(old);
            document.remove("_id");
            document.putAll(toDocument(quiz));
            document.put("sortOrder", old == null ? nextSortOrder() : number(old.get("sortOrder"), 0));
            document.put("createdAt", old == null ? Date.from(now) : old.getOrDefault("createdAt", Date.from(now)));
            document.put("updatedAt", Date.from(now));
            document.put("updatedBy", actorId);
            document.put("revision", oldRevision + 1);

            if (old == null) {
                transaction.create(QUIZZES, quiz.getId(), document);
            } else {
                transaction.set(QUIZZES, quiz.getId(), document);
            }
            String resolvedAction = action == null ? (old == null ? "quiz_created" : "quiz_updated") : action;
            transaction.create(AUDIT_LOGS, auditId,
                    auditEntry(actorId, resolvedAction, "quiz", quiz.getId(), quiz.getId(), summary, now,
                            describeChanges(old, quiz)));
            return null;
        });
        invalidateCache();
    }

    public List<Map<String, Object>> recentAuditLogs(int limit) {
        return store.findAll(AUDIT_LOGS).stream()
                .sorted(Comparator.comparing(this::auditTime, Comparator.reverseOrder()))
                .limit(Math.max(0, limit))
                .toList();
    }

    private int nextSortOrder() {
        return store.findAll(QUIZZES).stream()
                .mapToInt(document -> number(document.get("sortOrder"), 0))
                .max().orElse(-1) + 1;
    }

    private Map<String, Object> auditEntry(String actorId, String action, String targetType,
                                           String targetId, String quizId, String title, Instant at,
                                           List<Map<String, Object>> changes) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("actorId", actorId);
        entry.put("action", action);
        entry.put("targetType", targetType);
        entry.put("targetId", targetId);
        entry.put("quizId", quizId);
        entry.put("summary", title);
        entry.put("changes", changes);
        entry.put("timestamp", Date.from(at));
        return entry;
    }

    private List<Map<String, Object>> describeChanges(Map<String, Object> old, Quiz quiz) {
        List<Map<String, Object>> changes = new ArrayList<>();
        Map<String, Object> nextFields = new LinkedHashMap<>();
        nextFields.put("title", quiz.getTitle());
        nextFields.put("tagline", quiz.getTagline());
        nextFields.put("description", quiz.getDescription());
        nextFields.put("icon", quiz.getIcon());
        nextFields.put("difficulty", quiz.getLevel());
        nextFields.put("estimatedMinutes", quiz.getEstimatedMinutes());
        nextFields.put("accentColor", quiz.getAccentColor());
        nextFields.put("status", quiz.getStatus().firestoreValue());
        Map<String, String> labels = Map.of(
                "title", "Title",
                "tagline", "Tagline",
                "description", "Description",
                "icon", "Icon",
                "difficulty", "Level",
                "estimatedMinutes", "Estimated minutes",
                "accentColor", "Accent colour",
                "status", "Track status"
        );

        for (Map.Entry<String, Object> field : nextFields.entrySet()) {
            Object previous = old == null ? null : old.get(field.getKey());
            if (old == null || !java.util.Objects.equals(stringValue(previous), stringValue(field.getValue()))) {
                changes.add(auditChange(labels.get(field.getKey()), display(previous), display(field.getValue())));
            }
        }

        Map<String, Map<?, ?>> previousQuestions = questionDocuments(old);
        for (Question question : quiz.getQuestions()) {
            Map<?, ?> previous = previousQuestions.remove(question.getId());
            String currentSummary = questionSummary(question);
            if (previous == null) {
                changes.add(auditChange("Question added", "—", currentSummary));
            } else {
                String previousSummary = questionSummary(previous);
                if (!previousSummary.equals(currentSummary)) {
                    changes.add(auditChange("Question updated", previousSummary, currentSummary));
                }
            }
        }
        previousQuestions.values().forEach(removed -> changes.add(
                auditChange("Question removed", questionSummary(removed), "—")));
        return changes;
    }

    private Map<String, Map<?, ?>> questionDocuments(Map<String, Object> quizDocument) {
        Map<String, Map<?, ?>> byId = new LinkedHashMap<>();
        if (quizDocument == null || !(quizDocument.get("questions") instanceof List<?> questions)) {
            return byId;
        }
        for (Object item : questions) {
            if (item instanceof Map<?, ?> question && question.get("id") instanceof String id) {
                byId.put(id, question);
            }
        }
        return byId;
    }

    private String questionSummary(Question question) {
        String options = java.util.stream.IntStream.range(0, question.getOptions().size())
                .mapToObj(index -> (index + 1) + ". " + question.getOptions().get(index))
                .collect(java.util.stream.Collectors.joining("; "));
        return "Prompt: " + question.getPrompt() + " | Options: " + options
                + " | Correct: " + (question.getCorrectOptionIndex() + 1)
                + " | Difficulty: " + difficultyName(question.getDifficulty())
                + " | Status: " + question.getStatus().firestoreValue()
                + " | Explanation: " + question.getExplanation();
    }

    private String questionSummary(Map<?, ?> question) {
        List<String> options = strings(question.get("options"));
        String optionText = java.util.stream.IntStream.range(0, options.size())
                .mapToObj(index -> (index + 1) + ". " + options.get(index))
                .collect(java.util.stream.Collectors.joining("; "));
        int correct = number(question.get("correctOptionIndex"), 0) + 1;
        return "Prompt: " + string(question.get("prompt"), "") + " | Options: " + optionText
                + " | Correct: " + correct
                + " | Difficulty: " + difficultyName(number(question.get("difficulty"), 1))
                + " | Status: " + string(question.get("status"), "published")
                + " | Explanation: " + string(question.get("explanation"), "");
    }

    private String difficultyName(int difficulty) {
        return switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Medium";
            case 3 -> "Hard";
            default -> "Unknown";
        };
    }

    private Map<String, Object> auditChange(String field, String before, String after) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("before", before);
        change.put("after", after);
        return change;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String display(Object value) {
        return value == null ? "—" : value.toString();
    }

    private Instant auditTime(Map<String, Object> entry) {
        Object value = entry.get("timestamp");
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof com.google.cloud.Timestamp timestamp) {
            return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        }
        return Instant.EPOCH;
    }

    private Quiz publishedQuestionsOnly(Quiz quiz) {
        List<Question> questions = quiz.getQuestions().stream().filter(Question::isPublished).toList();
        return new Quiz(quiz.getId(), quiz.getTitle(), quiz.getTagline(), quiz.getDescription(), quiz.getIcon(),
                quiz.getLevel(), quiz.getEstimatedMinutes(), quiz.getAccentColor(), questions,
                quiz.getStatus(), quiz.getRevision());
    }

    private void invalidateCache() {
        cachedQuizzes = null;
        cachedAtMillis = 0;
    }

    private Quiz mapQuiz(Map<String, Object> document) {
        String quizId = (String) document.get("_id");
        Object rawQuestions = document.get("questions");
        List<?> questionDocuments = rawQuestions instanceof List<?> list ? list : List.of();
        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < questionDocuments.size(); index++) {
            if (!(questionDocuments.get(index) instanceof Map<?, ?> question)) {
                continue;
            }
            String questionId = (String) question.get("id");
            questions.add(new Question(
                    questionId,
                    string(question.get("prompt"), ""),
                    strings(question.get("options")),
                    number(question.get("correctOptionIndex"), 0),
                    string(question.get("explanation"), ""),
                    number(question.get("difficulty"),
                            QuizCatalog.difficultyForQuestion(quizId, questionId, index)),
                    ContentStatus.from(question.get("status"))
            ));
        }
        return new Quiz(
                quizId,
                string(document.get("title"), "Untitled quiz"),
                string(document.get("tagline"), ""),
                string(document.get("description"), ""),
                string(document.get("icon"), "?"),
                string(document.get("difficulty"), "Beginner"),
                number(document.get("estimatedMinutes"), 0),
                string(document.get("accentColor"), "#F26430"),
                questions,
                ContentStatus.from(document.get("status")),
                number(document.get("revision"), 0)
        );
    }

    private Map<String, Object> toDocument(Quiz quiz) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", quiz.getTitle());
        document.put("tagline", quiz.getTagline());
        document.put("description", quiz.getDescription());
        document.put("icon", quiz.getIcon());
        document.put("difficulty", quiz.getLevel());
        document.put("estimatedMinutes", quiz.getEstimatedMinutes());
        document.put("accentColor", quiz.getAccentColor());
        document.put("status", quiz.getStatus().firestoreValue());
        document.put("contentManaged", true);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (Question question : quiz.getQuestions()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", question.getId());
            item.put("prompt", question.getPrompt());
            item.put("options", question.getOptions());
            item.put("correctOptionIndex", question.getCorrectOptionIndex());
            item.put("difficulty", question.getDifficulty());
            item.put("explanation", question.getExplanation());
            item.put("status", question.getStatus().firestoreValue());
            questions.add(item);
        }
        document.put("questions", questions);
        return document;
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> item == null ? "" : item.toString()).toList();
    }

    private String string(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
