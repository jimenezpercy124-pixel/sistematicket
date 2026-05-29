package com.siit.ticket.controller;

import com.siit.ticket.security.JwtUtil;
import com.siit.ticket.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Endpoint SSE autenticado por token en query parameter.
     * EventSource del navegador no soporta headers, por eso se pasa el token por URL.
     * Ejemplo: /api/notifications/stream?token=eyJhbGci...
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(@RequestParam(required = false) String token) {
        if (token == null || token.isEmpty()) {
            log.warn("SSE: Intento de conexión sin token");
            return ResponseEntity.status(401).build();
        }

        try {
            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractClaim(token, claims -> claims.get("id", Long.class));

            if (username == null || userId == null) {
                log.warn("SSE: Token inválido - datos incompletos");
                return ResponseEntity.status(401).build();
            }

            log.info("SSE: Usuario {} (ID: {}) conectándose al stream", username, userId);
            SseEmitter emitter = notificationService.subscribe(userId);
            return ResponseEntity.ok(emitter);

        } catch (Exception e) {
            log.warn("SSE: Token inválido o expirado: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}
