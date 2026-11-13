package com.quizly.service;

import com.google.gson.Gson;
import com.quizly.model.Result;
import com.quizly.model.RoomPlayerSnapshot;
import com.quizly.model.RoomSnapshot;
import com.quizly.model.User;
import com.quizly.repository.QuizRepository;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates ephemeral, room-code multiplayer quizzes for one app instance.
 */
public final class RoomService implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(RoomService.class.getName());
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_PLAYERS = 20;
    private static final long ROOM_LIFETIME_SECONDS = 10_800;

    private final QuizRepository quizzes;
    private final SecureRandom random = new SecureRandom();
    private final Gson gson = new Gson();
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    public RoomService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    public synchronized RoomSnapshot create(User host, String quizId) {
        var quiz = quizzes.findPublishedById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Choose a valid quiz for the room."));
        purgeExpired();
        String code;
        do {
            code = generateCode();
        } while (rooms.containsKey(code));

        RoomState room = new RoomState(code, quiz.getId(), quiz.getTitle(), host.getId(),
                Math.min(QuizService.QUESTIONS_PER_ATTEMPT, quiz.getQuestionCount()));
        room.players.put(host.getId(), new PlayerState(host.getName()));
        rooms.put(code, room);
        return snapshot(room);
    }

    public synchronized RoomSnapshot join(String code, User player) {
        RoomState room = requireRoom(code);
        boolean alreadyJoined = room.players.containsKey(player.getId());
        if (room.started && !alreadyJoined) {
            throw new IllegalStateException("This room has already started.");
        }
        if (!alreadyJoined && room.players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("This room is full.");
        }
        room.players.putIfAbsent(player.getId(), new PlayerState(player.getName()));
        broadcast(room, "state");
        return snapshot(room);
    }

    public synchronized RoomSnapshot find(String code, String userId) {
        RoomState room = requireRoom(code);
        requirePlayer(room, userId);
        return snapshot(room);
    }

    public synchronized RoomSnapshot start(String code, String userId) {
        RoomState room = requireRoom(code);
        requirePlayer(room, userId);
        if (!room.hostUserId.equals(userId)) {
            throw new IllegalStateException("Only the room host can start the quiz.");
        }
        if (room.players.size() < 2) {
            throw new IllegalStateException("Invite at least one other player before starting.");
        }
        if (room.started) {
            throw new IllegalStateException("This room has already started.");
        }
        room.started = true;
        broadcast(room, "started");
        return snapshot(room);
    }

    public synchronized void recordProgress(String code, String userId, int answered) {
        RoomState room = requireRoom(code);
        if (!room.started) {
            throw new IllegalStateException("The room quiz has not started.");
        }
        PlayerState player = requirePlayer(room, userId);
        player.answered = Math.max(player.answered, Math.min(answered, room.totalQuestions));
        broadcast(room, "state");
    }

    public synchronized void completeAttempt(String code, String userId, Result result) {
        RoomState room = requireRoom(code);
        PlayerState player = requirePlayer(room, userId);
        player.answered = room.totalQuestions;
        player.finished = true;
        player.points = result.getPoints();
        player.percentage = result.getPercentage();
        player.completedAt = result.getCompletedAt();
        broadcast(room, "state");
    }

    public synchronized String createConnectionToken(String code, String userId) {
        RoomState room = requireRoom(code);
        requirePlayer(room, userId);
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        room.connectionTokens.put(userId, token);
        return token;
    }

    public synchronized String connectWithToken(String code, String token, Session session) {
        RoomState room = requireRoom(code);
        if (token == null) {
            throw new IllegalStateException("Refresh the room page to reconnect.");
        }
        String userId = room.connectionTokens.entrySet().stream()
                .filter(entry -> MessageDigest.isEqual(
                        entry.getValue().getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Refresh the room page to reconnect."));
        room.connectionTokens.remove(userId);
        room.connections.put(userId, session);
        send(session, "state", room);
        return userId;
    }

    public synchronized void disconnect(String code, String userId, Session session) {
        RoomState room = rooms.get(code);
        if (room != null && room.connections.get(userId) == session) {
            room.connections.remove(userId);
        }
    }

    private RoomState requireRoom(String code) {
        purgeExpired();
        String normalizedCode = code == null ? "" : code.strip().toUpperCase(java.util.Locale.ROOT);
        if (!normalizedCode.matches("[A-HJ-NP-Z2-9]{6}")) {
            throw new IllegalArgumentException("Enter a valid six-character room code.");
        }
        RoomState room = rooms.get(normalizedCode);
        if (room == null) {
            throw new IllegalArgumentException("That room code was not found or has expired.");
        }
        return room;
    }

    private PlayerState requirePlayer(RoomState room, String userId) {
        PlayerState player = room.players.get(userId);
        if (player == null) {
            throw new IllegalStateException("Join this room before continuing.");
        }
        return player;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private RoomSnapshot snapshot(RoomState room) {
        List<RoomPlayerSnapshot> players = room.players.entrySet().stream()
                .map(entry -> new RoomPlayerSnapshot(
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().answered,
                        room.totalQuestions,
                        entry.getValue().finished,
                        entry.getValue().points,
                        entry.getValue().percentage,
                        entry.getValue().completedAt
                ))
                .sorted(Comparator.comparing(RoomPlayerSnapshot::isFinished).reversed()
                        .thenComparing(RoomPlayerSnapshot::getPoints, Comparator.reverseOrder())
                        .thenComparing(RoomPlayerSnapshot::getCompletedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RoomPlayerSnapshot::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RoomSnapshot(room.code, room.quizId, room.quizTitle, room.hostUserId, room.started, players);
    }

    private void broadcast(RoomState room, String event) {
        for (Session session : new ArrayList<>(room.connections.values())) {
            send(session, event, room);
        }
    }

    private void send(Session session, String event, RoomState room) {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        RoomSnapshot roomSnapshot = snapshot(room);
        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("started", roomSnapshot.isStarted());
        roomUpdate.put("players", roomSnapshot.getPlayers().stream()
                .map(player -> Map.of(
                        "name", player.getName(),
                        "answered", player.getAnswered(),
                        "total", player.getTotal(),
                        "finished", player.isFinished(),
                        "points", player.getPoints(),
                        "percentage", player.getPercentage()))
                .toList());
        message.put("room", roomUpdate);
        session.getAsyncRemote().sendText(gson.toJson(message), result -> {
            if (!result.isOK()) {
                LOG.log(Level.WARNING, "Could not send a live room update", result.getException());
            }
        });
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minusSeconds(ROOM_LIFETIME_SECONDS);
        List<String> expiredCodes = rooms.values().stream()
                .filter(room -> room.createdAt.isBefore(cutoff))
                .map(room -> room.code)
                .toList();
        for (String code : expiredCodes) {
            RoomState expired = rooms.remove(code);
            if (expired != null) {
                for (Session session : expired.connections.values()) {
                    try {
                        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Room expired"));
                    } catch (IOException exception) {
                        LOG.log(Level.WARNING, "Could not close an expired room connection", exception);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        for (RoomState room : rooms.values()) {
            for (Session session : room.connections.values()) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Application stopped"));
                } catch (IOException exception) {
                    LOG.log(Level.WARNING, "Could not close a multiplayer room connection", exception);
                }
            }
        }
        rooms.clear();
    }

    private static final class RoomState {
        private final String code;
        private final String quizId;
        private final String quizTitle;
        private final String hostUserId;
        private final int totalQuestions;
        private final Instant createdAt = Instant.now();
        private final Map<String, PlayerState> players = new HashMap<>();
        private final Map<String, Session> connections = new HashMap<>();
        private final Map<String, String> connectionTokens = new HashMap<>();
        private boolean started;

        private RoomState(String code, String quizId, String quizTitle, String hostUserId, int totalQuestions) {
            this.code = code;
            this.quizId = quizId;
            this.quizTitle = quizTitle;
            this.hostUserId = hostUserId;
            this.totalQuestions = totalQuestions;
        }
    }

    private static final class PlayerState {
        private final String name;
        private int answered;
        private boolean finished;
        private int points;
        private int percentage;
        private Instant completedAt;

        private PlayerState(String name) {
            this.name = name;
        }
    }
}
