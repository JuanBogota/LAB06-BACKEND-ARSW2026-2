package edu.eci.arsw.blueprints.realtime;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Adaptador de observabilidad: registra conexiones y desconexiones STOMP
 * y lleva la cuenta de sesiones activas. No conoce el dominio.
 */
@Component
public class WebSocketSessionLogger {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionLogger.class);

    private final AtomicInteger activeSessions = new AtomicInteger();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        int active = activeSessions.incrementAndGet();
        log.info("WebSocket conectado: sesion={} | sesiones activas={}", sessionId, active);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        int active = activeSessions.updateAndGet(n -> Math.max(0, n - 1));
        log.info("WebSocket desconectado: sesion={} | sesiones activas={}", event.getSessionId(), active);
    }
}