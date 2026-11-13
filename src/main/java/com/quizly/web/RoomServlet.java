package com.quizly.web;

import com.quizly.model.Attempt;
import com.quizly.model.RoomPlayerSnapshot;
import com.quizly.model.User;
import com.quizly.repository.QuizRepository;
import com.quizly.service.RoomService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "RoomServlet", urlPatterns = {"/room", "/room/create", "/room/join", "/room/start"})
public final class RoomServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebSupport.noCache(response);
        User user = WebSupport.currentUser(request);
        String code = request.getParameter("code");
        request.setAttribute(App.ACTIVE_NAV, "room");
        if (code != null && !code.isBlank()) {
            try {
                var room = WebSupport.<RoomService>service(request, App.ROOM_SERVICE)
                        .find(code.strip().toUpperCase(java.util.Locale.ROOT), user.getId());
                HttpSession session = request.getSession(false);
                Attempt attempt = session == null ? null : (Attempt) session.getAttribute(App.ATTEMPT);
                request.setAttribute("room", room);
                request.setAttribute("roomIsHost", room.getHostUserId().equals(user.getId()));
                request.setAttribute("roomPlayerFinished", room.getPlayers().stream()
                        .filter(player -> player.getUserId().equals(user.getId()))
                        .findFirst().map(RoomPlayerSnapshot::isFinished).orElse(false));
                request.setAttribute("roomAttemptActive",
                        attempt != null && room.getCode().equals(session.getAttribute(App.ROOM_CODE)));
                request.setAttribute("roomWebSocketToken",
                        WebSupport.<RoomService>service(request, App.ROOM_SERVICE)
                                .createConnectionToken(room.getCode(), user.getId()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                WebSupport.flash(request.getSession(), App.FLASH_ERROR, exception.getMessage());
                WebSupport.redirect(request, response, "/room");
                return;
            }
        } else {
            QuizRepository quizzes = WebSupport.service(request, App.QUIZ_REPOSITORY);
            request.setAttribute("quizzes", quizzes.findPublished());
        }
        WebSupport.forward(request, response, "room");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !WebSupport.isCsrfValid(request)) {
            WebSupport.flash(request.getSession(), App.FLASH_ERROR, "Your session expired. Please try again.");
            WebSupport.redirect(request, response, "/room");
            return;
        }
        User user = WebSupport.currentUser(request);
        RoomService rooms = WebSupport.service(request, App.ROOM_SERVICE);
        String path = request.getServletPath();
        try {
            if ("/room/create".equals(path)) {
                var room = rooms.create(user, request.getParameter("quizId"));
                session.setAttribute(App.ROOM_CODE, room.getCode());
                response.sendRedirect(request.getContextPath() + "/room?code=" + room.getCode());
                return;
            }
            if ("/room/join".equals(path)) {
                var room = rooms.join(request.getParameter("code"), user);
                session.setAttribute(App.ROOM_CODE, room.getCode());
                response.sendRedirect(request.getContextPath() + "/room?code=" + room.getCode());
                return;
            }
            if ("/room/start".equals(path)) {
                Object code = session.getAttribute(App.ROOM_CODE);
                if (!(code instanceof String roomCode)) {
                    throw new IllegalStateException("Create or join a room before starting.");
                }
                rooms.start(roomCode, user.getId());
                response.sendRedirect(request.getContextPath() + "/room?code=" + roomCode);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            WebSupport.flash(session, App.FLASH_ERROR, exception.getMessage());
            Object code = session.getAttribute(App.ROOM_CODE);
            String redirect = code instanceof String roomCode
                    ? "/room?code=" + URLEncoder.encode(roomCode, StandardCharsets.UTF_8)
                    : "/room";
            WebSupport.redirect(request, response, redirect);
        }
    }
}
