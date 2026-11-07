package com.quizly.web;

/**
 * Central names for objects shared through servlet-context/session scope.
 * These constants prevent typo bugs in multiple servlets and JSPs.
 */
public final class App {

    public static final String DATABASE = "database";
    public static final String QUIZ_REPOSITORY = "quizRepository";
    public static final String USER_REPOSITORY = "userRepository";
    public static final String QUIZ_SERVICE = "quizService";
    public static final String LEADERBOARD_SERVICE = "leaderboardService";
    public static final String AUTH_SERVICE = "authService";
    public static final String RESULT_REPOSITORY = "resultRepository";
    public static final String PROGRESSION_REPOSITORY = "progressionRepository";
    public static final String ROOM_SERVICE = "roomService";

    public static final String ATTEMPT = "attempt";
    public static final String LAST_RESULT = "lastResult";
    public static final String LAST_RESULT_ID = "lastResultId";
    public static final String LAST_XP_AWARD = "lastXpAward";
    public static final String ROOM_CODE = "roomCode";
    public static final String LAST_ROOM_CODE = "lastRoomCode";
    public static final String PLAYER_NAME = "playerName";
    public static final String CURRENT_USER = "currentUser";
    public static final String CSRF_TOKEN = "csrfToken";

    public static final String FLASH_SUCCESS = "flashSuccess";
    public static final String FLASH_ERROR = "flashError";

    public static final String ACTIVE_NAV = "activeNav";

    private App() {
    }
}
