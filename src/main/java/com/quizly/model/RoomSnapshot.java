package com.quizly.model;

import java.util.List;

public final class RoomSnapshot {

    private final String code;
    private final String quizId;
    private final String quizTitle;
    private final String hostUserId;
    private final boolean started;
    private final List<RoomPlayerSnapshot> players;

    public RoomSnapshot(String code, String quizId, String quizTitle, String hostUserId,
                        boolean started, List<RoomPlayerSnapshot> players) {
        this.code = code;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.hostUserId = hostUserId;
        this.started = started;
        this.players = List.copyOf(players);
    }

    public String getCode() {
        return code;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public String getHostUserId() {
        return hostUserId;
    }

    public boolean isStarted() {
        return started;
    }

    public List<RoomPlayerSnapshot> getPlayers() {
        return players;
    }
}
