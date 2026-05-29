package com.siit.ticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@SuppressWarnings("null")
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Mapa thread-safe: userId -> SseEmitter (cada usuario tiene su propio canal)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // Ejecutor para que el envío de eventos sea asíncrono y no bloquee el sistema
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Suscribe a un usuario autenticado al canal de notificaciones SSE.
     * @param userId El ID del usuario autenticado
     * @return SseEmitter con timeout de 5 minutos
     */
    public SseEmitter subscribe(Long userId) {
        // Si ya tiene una conexión activa, cerrarla primero
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception ignored) {}
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutos para evitar desconexiones frecuentes
        
        this.emitters.put(userId, emitter);
        log.info("SSE: Usuario {} conectado. Total conexiones activas: {}", userId, emitters.size());

        emitter.onCompletion(() -> {
            this.emitters.remove(userId);
            log.debug("SSE: Conexión completada para usuario {}", userId);
        });
        emitter.onTimeout(() -> {
            this.emitters.remove(userId);
            log.debug("SSE: Timeout para usuario {}", userId);
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        emitter.onError(e -> {
            this.emitters.remove(userId);
            log.debug("SSE: Error para usuario {}: {}", userId, e.getMessage());
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });

        // Enviar un ping inicial inmediato para validar la conexión
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (Exception e) {
            this.emitters.remove(userId);
            log.warn("SSE: Falló ping inicial para usuario {}", userId);
        }

        return emitter;
    }

    // Método para enviar pings periódicos cada 15 segundos y evitar timeouts del navegador/proxy
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (Exception e) {
                Long userId = entry.getKey();
                SseEmitter emitter = entry.getValue();
                this.emitters.remove(userId);
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Envía un evento a un usuario específico.
     * @param userId ID del usuario destino
     * @param eventName Nombre del evento SSE
     * @param eventData Datos del evento
     */
    public void dispatchEventToUser(Long userId, String eventName, Object eventData) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            executor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(eventData));
                } catch (Exception e) {
                    emitters.remove(userId);
                    log.debug("SSE: Falló envío a usuario {}, removiendo conexión", userId);
                }
            });
        }
    }

    /**
     * Envía un evento a múltiples usuarios específicos.
     * @param userIds Set de IDs de usuarios destino
     * @param eventName Nombre del evento SSE
     * @param eventData Datos del evento
     */
    public void dispatchEventToUsers(Set<Long> userIds, String eventName, Object eventData) {
        for (Long userId : userIds) {
            dispatchEventToUser(userId, eventName, eventData);
        }
    }

    /**
     * Envía un evento a TODOS los usuarios conectados (broadcast).
     * Útil para eventos globales como nuevo_usuario.
     * @param eventName Nombre del evento SSE
     * @param eventData Datos del evento
     */
    public void dispatchEvent(String eventName, Object eventData) {
        executor.execute(() -> {
            for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event()
                            .name(eventName)
                            .data(eventData));
                } catch (Exception e) {
                    emitters.remove(entry.getKey());
                }
            }
        });
    }
}
