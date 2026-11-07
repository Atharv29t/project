package com.quizly.web;

import com.quizly.service.RoomService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint(value = "/ws/room/{code}/{token}", configurator = RoomEndpointConfigurator.class)
public final class RoomEndpoint {

    private static final Logger LOG = Logger.getLogger(RoomEndpoint.class.getName());

    @OnOpen
    public void onOpen(Session socket, EndpointConfig config,
                       @PathParam("code") String code, @PathParam("token") String token) throws IOException {
        RoomService rooms = RoomEndpointConfigurator.getRoomService();
        if (rooms == null) {
            socket.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Room service is unavailable"));
            return;
        }
        try {
            String userId = rooms.connectWithToken(code, token, socket);
            socket.getUserProperties().put("roomCode", code);
            socket.getUserProperties().put("userId", userId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            socket.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, exception.getMessage()));
        }
    }

    @OnClose
    public void onClose(Session socket) {
        String code = (String) socket.getUserProperties().get("roomCode");
        String userId = (String) socket.getUserProperties().get("userId");
        RoomService rooms = RoomEndpointConfigurator.getRoomService();
        if (code != null && userId != null && rooms != null) {
            rooms.disconnect(code, userId, socket);
        }
    }

    @OnError
    public void onError(Session socket, Throwable error) {
        LOG.log(Level.WARNING, "Multiplayer room WebSocket failed", error);
    }
}
