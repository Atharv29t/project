package com.quizly.web;

import com.quizly.service.RoomService;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.net.URI;
import java.util.List;

public final class RoomEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private static volatile RoomService roomService;

    static void setRoomService(RoomService service) {
        roomService = service;
    }

    static RoomService getRoomService() {
        return roomService;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        List<String> origins = request.getHeaders().get("Origin");
        URI requestUri = request.getRequestURI();
        if (origins == null || origins.isEmpty() || !sameOrigin(origins.get(0), requestUri)) {
            throw new IllegalStateException("Cross-origin room connections are not allowed.");
        }
    }

    private boolean sameOrigin(String origin, URI requestUri) {
        try {
            URI originUri = URI.create(origin);
            String scheme = requestUri.getScheme();
            String requestScheme = "wss".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                    ? "https" : "http";
            int requestPort = requestUri.getPort() < 0
                    ? ("https".equals(requestScheme) ? 443 : 80) : requestUri.getPort();
            int originPort = originUri.getPort() < 0
                    ? ("https".equalsIgnoreCase(originUri.getScheme()) ? 443 : 80) : originUri.getPort();
            return requestScheme.equalsIgnoreCase(originUri.getScheme())
                    && requestUri.getHost() != null
                    && requestUri.getHost().equalsIgnoreCase(originUri.getHost())
                    && requestPort == originPort;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
