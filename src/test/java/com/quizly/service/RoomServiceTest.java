package com.quizly.service;

import com.quizly.TestDatabase;
import com.quizly.model.AnswerReview;
import com.quizly.model.Result;
import com.quizly.model.User;
import com.quizly.repository.QuizRepository;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTest {

    @Test
    void createsRoomJoinsPlayersStartsAndBroadcastsScoresInSnapshots() {
        var database = TestDatabase.create("rooms-" + UUID.randomUUID());
        database.initialize();
        RoomService service = new RoomService(new QuizRepository(database));
        User host = user("host", "Host");
        User guest = user("guest", "Guest");

        var created = service.create(host, "java");
        assertTrue(created.getCode().matches("[A-HJ-NP-Z2-9]{6}"));
        assertThrows(IllegalStateException.class, () -> service.start(created.getCode(), host.getId()));

        var joined = service.join(created.getCode().toLowerCase(), guest);
        assertEquals(2, joined.getPlayers().size());
        assertThrows(IllegalStateException.class, () -> service.start(created.getCode(), guest.getId()));

        var started = service.start(created.getCode(), host.getId());
        assertTrue(started.isStarted());
        assertThrows(IllegalStateException.class, () -> service.join(created.getCode(), user("late", "Late")));
        assertEquals(2, service.join(created.getCode(), guest).getPlayers().size());

        Result result = new Result("result", guest.getId(), guest.getName(), "java",
                "Java Fundamentals", "☕", "#38BDF8", 5, 6, 83,
                Instant.now(), List.of(new AnswerReview(1, "Q", "A", "A", "Why")));
        service.recordProgress(created.getCode(), host.getId(), 2);
        service.completeAttempt(created.getCode(), guest.getId(), result);

        var snapshot = service.find(created.getCode(), host.getId());
        var completedGuest = snapshot.getPlayers().stream()
                .filter(player -> player.getUserId().equals(guest.getId()))
                .findFirst().orElseThrow();
        assertTrue(completedGuest.isFinished());
        assertEquals(83, completedGuest.getPercentage());
        assertEquals(5, completedGuest.getPoints());
        assertFalse(snapshot.getPlayers().get(1).isFinished());
        service.close();
    }

    @Test
    void websocketConnectionTokensCanOnlyBeUsedOnce() {
        var database = TestDatabase.create("rooms-" + UUID.randomUUID());
        database.initialize();
        RoomService service = new RoomService(new QuizRepository(database));
        User host = user("host", "Host");
        var room = service.create(host, "java");
        String token = service.createConnectionToken(room.getCode(), host.getId());

        assertEquals(host.getId(), service.connectWithToken(room.getCode(), token, openSession()));
        assertThrows(IllegalStateException.class,
                () -> service.connectWithToken(room.getCode(), token, openSession()));
        service.close();
    }

    private Session openSession() {
        RemoteEndpoint.Async remote = (RemoteEndpoint.Async) Proxy.newProxyInstance(
                RemoteEndpoint.Async.class.getClassLoader(),
                new Class<?>[]{RemoteEndpoint.Async.class},
                (proxy, method, args) -> null);
        return (Session) Proxy.newProxyInstance(
                Session.class.getClassLoader(),
                new Class<?>[]{Session.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isOpen" -> true;
                    case "getAsyncRemote" -> remote;
                    default -> null;
                });
    }

    private User user(String id, String name) {
        return new User(id, name, id + "@example.com", "hash", Instant.now(), Instant.now());
    }
}
