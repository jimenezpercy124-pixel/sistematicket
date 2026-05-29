package com.siit.ticket.service;

import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.User;
import com.siit.ticket.model.Incidencia;
import com.siit.ticket.repository.IncidenciaRepository;
import com.siit.ticket.repository.TicketRepository;
import com.siit.ticket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.siit.ticket.model.TicketSequence;
import com.siit.ticket.repository.TicketSequenceRepository;
import com.siit.ticket.model.TicketLog;
import com.siit.ticket.repository.TicketLogRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;

@Service
@SuppressWarnings("null")
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private TicketSequenceRepository sequenceRepository;

    @Autowired
    private TicketLogRepository ticketLogRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${ticket.confirmation-token.expiration-hours:72}")
    private int tokenExpirationHours;

    private synchronized String generateCustomId(String codIre) {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("MMyy")); // MMYY format
        
        TicketSequence sequence = sequenceRepository.findById(codIre)
                .orElse(new TicketSequence(codIre, 0L));
        
        long nextValue = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(nextValue);
        sequenceRepository.save(sequence);
        
        return String.format("TK-%s-%s-%07d", codIre, datePart, nextValue);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsByCreador(User creador) {
        return ticketRepository.findByCreador(creador);
    }

    public List<Ticket> getTicketsByIre(String codIre) {
        return ticketRepository.findByCodIre(codIre);
    }

    public List<Ticket> getTicketsByEspecialista(Integer num) {
        return ticketRepository.findByEspecialistaNum(num);
    }

    public Ticket createTicket(Ticket ticket) {
        Ticket updated = ticketRepository.save(ticket);
        notifyTicketParties(updated, "ticket_actualizado");
        return updated;
    }

    /** Asigna el especialista_num de la incidencia al ticket (antes de guardarlo) */
    public void asignarEspecialistaPorIncidencia(Ticket ticket, Long idIncidencia) {
        incidenciaRepository.findById(idIncidencia).ifPresent(inc -> {
            ticket.setIncidencia(inc);
            if (inc.getEspecialistaNum() != null) {
                ticket.setEspecialistaNum(inc.getEspecialistaNum());
            }
        });
    }

    private void sendStyledEmail(User user, String title, String subtitle, String content, String color, String idTicket, String status) {
        if (user == null) {
            log.warn("No se puede enviar correo: Usuario es NULL");
            return;
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Usuario {} no tiene correo configurado", user.getUsername());
            return;
        }
        
        log.info("Enviando correo a: {}", user.getEmail());
        CompletableFuture.runAsync(() -> {
            String body = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
                        + "  <div style='background-color: " + color + "; color: white; padding: 25px; text-align: center;'>"
                        + "    <h2 style='margin: 0; font-size: 24px;'>" + title + "</h2>"
                        + "    <p style='margin: 5px 0 0; opacity: 0.8; font-size: 14px;'>" + subtitle + "</p>"
                        + "  </div>"
                        + "  <div style='padding: 30px; color: #2d3436; line-height: 1.6;'>"
                        + "    <h3 style='color: " + color + "; margin-top: 0;'>¡Hola, " + user.getUsername() + "!</h3>"
                        + "    <p style='font-size: 15px;'>" + content + "</p>"
                        + "    <table style='width: 100%; background: #f8f9fa; padding: 20px; border-radius: 12px; margin: 20px 0; border-collapse: collapse;'>"
                        + "      <tr><td style='padding: 8px 0;'><b>ID Ticket:</b></td><td style='color: #e74c3c; font-weight: bold;'>" + idTicket + "</td></tr>"
                        + "      <tr><td style='padding: 8px 0;'><b>Estado Actual:</b></td><td><span style='background: " + color + "; color: white; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: bold;'>" + status + "</span></td></tr>"
                        + "    </table>"
                        + "    <p style='font-size: 13px; color: #636e72; text-align: center; margin-top: 25px; border-top: 1px solid #eee; padding-top: 20px;'><b>Sistema de Ticket - SIIT - SUNAFIL</b><br>"
                        + "    <i style='font-size: 11px; opacity: 0.8;'>Este es un mensaje automático, por favor no responda a este correo.</i></p>"
                        + "  </div>"
                        + "</div>";
            emailService.sendEmail(user.getEmail(), "SIIT - " + title + ": " + idTicket, body);
        });
    }

    @Transactional
    public Ticket createTicketWithFile(Ticket ticket, MultipartFile file) {
        if (ticket.getIdTicket() == null || ticket.getIdTicket().isEmpty()) {
            ticket.setIdTicket(generateCustomId(ticket.getCodIre()));
        }

        if (file != null && !file.isEmpty()) {
            String filename = fileService.save(file);
            ticket.setLinkDrive(filename);
        }
        Ticket saved = ticketRepository.save(ticket);
        logEvent(saved.getIdTicket(), null, saved.getEstado(), "Creación inicial del ticket.");

        // Notificaciones SSE dirigidas
        Set<Long> targetUsers = new HashSet<>();
        if (saved.getCreador() != null) targetUsers.add(saved.getCreador().getIdUsuario());
        if (saved.getEspecialistaNum() != null) targetUsers.add(Long.valueOf(saved.getEspecialistaNum()));
        notificationService.dispatchEventToUsers(targetUsers, "nuevo_ticket", saved);
        // Broadcast para admins que tengan el dashboard abierto
        notificationService.dispatchEvent("nuevo_ticket", saved);
        
        // Correo al Inspector
        sendStyledEmail(saved.getCreador(), "Ticket Registrado", "Tu solicitud ha sido recibida", 
            "Hemos registrado correctamente tu ticket. Un especialista lo revisará a la brevedad.", 
            "#b02a2a", saved.getIdTicket(), "PENDIENTE");

        // Correo al Especialista
        if (saved.getEspecialistaNum() != null) {
            Optional<User> espOpt = userRepository.findById(Long.valueOf(saved.getEspecialistaNum()));
            if (espOpt.isPresent()) {
                User esp = espOpt.get();
                log.info("Enviando correo a especialista: {} ({})", esp.getUsername(), esp.getEmail());
                sendStyledEmail(esp, "Nuevo Ticket Asignado", "Tienes una tarea pendiente", 
                    "Se te ha asignado un nuevo ticket (<b>" + saved.getAsunto() + "</b>) para su atención.", 
                    "#2d3436", saved.getIdTicket(), "NUEVO");
            } else {
                log.warn("No se encontró especialista con NUM: {}", saved.getEspecialistaNum());
            }
        }
        
        return saved;
    }

    @Transactional
    public Ticket updateStatus(String id, String status) {
        return updateStatus(id, status, null);
    }

    @Transactional
    public Ticket updateStatus(String id, String status, String motivo) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        String prevEstado = ticket.getEstado();
        
        if ("RESUELTO".equals(status)) {
            // Especialista marca como resuelto -> Pasa a confirmación del Inspector
            ticket.setEstado("PENDIENTE_CONFIRMACION");
            String token = java.util.UUID.randomUUID().toString();
            ticket.setTokenConfirmacion(token);
            ticket.setFechaExpiracionToken(LocalDateTime.now().plusHours(tokenExpirationHours));
            Ticket saved = ticketRepository.save(ticket);
            logEvent(saved.getIdTicket(), prevEstado, "PENDIENTE_CONFIRMACION", "Especialista marcó el ticket como RESUELTO. Esperando confirmación.");
            notifyTicketParties(saved, "ticket_actualizado");

            // Enviar correo informativo al inspector
            if (saved.getCreador() != null && saved.getCreador().getEmail() != null) {
                CompletableFuture.runAsync(() -> {
                    String body = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden;'>"
                                + "  <div style='background-color: #27ae60; color: white; padding: 25px; text-align: center;'>"
                                + "    <h2 style='margin: 0;'>¡Atención Finalizada!</h2>"
                                + "    <p style='margin: 5px 0 0; opacity: 0.9;'>Su ticket ha sido marcado como resuelto</p>"
                                + "  </div>"
                                + "  <div style='padding: 30px; color: #2d3436; line-height: 1.6;'>"
                                + "    <p>Hola <b>" + saved.getCreador().getUsername() + "</b>,</p>"
                                + "    <p>El especialista ha resuelto su ticket <b>" + saved.getIdTicket() + "</b>. Por favor, ingrese al sistema para <b>confirmar</b> o <b>rechazar</b> la resolución recibida.</p>"
                                + "    <div style='margin: 30px 0; text-align: center;'>"
                                + "      <a href='" + frontendUrl + "' style='background: #b02a2a; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold;'>INGRESAR AL SISTEMA</a>"
                                + "    </div>"
                                + "    <p style='font-size: 13px; color: #636e72; text-align: center; margin-top: 25px; border-top: 1px solid #eee; padding-top: 20px;'><b>Sistema de Ticket - SIIT - SUNAFIL</b><br>"
                                + "    <i style='font-size: 11px; opacity: 0.8;'>Este es un mensaje automático, por favor no responda a este correo.</i></p>"
                                + "  </div>"
                                + "</div>";
                    emailService.sendEmail(saved.getCreador().getEmail(), "SIIT - Resolución de Ticket: " + saved.getIdTicket(), body);
                });
            }
            return saved;
        }

        ticket.setEstado(status);
        if ("RECHAZADO".equals(status)) {
            ticket.setMotivoRechazo(motivo);
            ticket.setFechaResolucion(LocalDateTime.now());
            if (motivo != null && !motivo.trim().isEmpty()) {
                ticket.setAsunto(ticket.getAsunto() + "\n\n--- RECHAZO DE ESPECIALISTA ---\n" + motivo);
            }
        }
        Ticket updated = ticketRepository.save(ticket);
        
        String details = "Estado del ticket actualizado a: " + status;
        if ("RECHAZADO".equals(status)) {
            details = "Especialista rechazó el ticket. Motivo: " + motivo;
        }
        logEvent(updated.getIdTicket(), prevEstado, status, details);
        
        notifyTicketParties(updated, "ticket_actualizado");
        
        if ("RECHAZADO".equals(status)) {
            sendStyledEmail(updated.getCreador(), "Ticket Rechazado ❌", "Tu solicitud ha sido rechazada por el especialista", 
                "El especialista ha rechazado atender tu ticket.<br><b>Motivo de rechazo:</b> " + motivo, 
                "#ef4444", updated.getIdTicket(), status);
        } else {
            sendStyledEmail(updated.getCreador(), "Ticket Actualizado", "Hay novedades en tu solicitud", 
                "Tu ticket ha cambiado al estado: <b style='color: #2980b9;'>" + status + "</b>", 
                "#2980b9", updated.getIdTicket(), status);
        }
        
        return updated;
    }

    @Transactional
    public Map<String, Object> processConfirmation(String token, String accion, String motivo, MultipartFile evidencia) {
        Map<String, Object> response = new HashMap<>();
        Optional<Ticket> ticketOpt = ticketRepository.findByTokenConfirmacion(token);
        
        if (ticketOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Esta solicitud ya ha sido procesada o el enlace ha expirado.");
            return response;
        }

        Ticket ticket = ticketOpt.get();
        if (ticket.getFechaExpiracionToken() != null && ticket.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            // Limpiar token y expiración
            ticket.setTokenConfirmacion(null);
            ticket.setFechaExpiracionToken(null);
            ticketRepository.save(ticket);
            
            response.put("success", false);
            response.put("message", "El enlace ha expirado por motivos de seguridad. Por favor, inicie sesión para confirmar o rechazar.");
            return response;
        }
        return finalizeConfirmation(ticket, accion, motivo, evidencia);
    }

    @Transactional
    public Map<String, Object> processDirectConfirmation(String idTicket, String accion, String motivo, MultipartFile evidencia, User currentUser) {
        Ticket ticket = ticketRepository.findById(idTicket)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        if (!ticket.getCreador().getIdUsuario().equals(currentUser.getIdUsuario())) {
            throw new RuntimeException("No tiene permisos para confirmar este ticket");
        }

        return finalizeConfirmation(ticket, accion, motivo, evidencia);
    }

    private Map<String, Object> finalizeConfirmation(Ticket ticket, String accion, String motivo, MultipartFile evidencia) {
        Map<String, Object> response = new HashMap<>();
        response.put("id_ticket", ticket.getIdTicket());
        String prevEstado = ticket.getEstado();

        if ("confirmar".equals(accion)) {
            ticket.setEstado("RESUELTO");
            ticket.setFechaResolucion(LocalDateTime.now());
            ticket.setTokenConfirmacion(null);
            ticket.setFechaExpiracionToken(null);
            Ticket saved = ticketRepository.save(ticket);
            logEvent(saved.getIdTicket(), prevEstado, "RESUELTO", "Inspector confirmó la resolución. Ticket cerrado.");
            notifyTicketParties(saved, "ticket_actualizado");
            response.put("success", true);
            response.put("message", "Ticket cerrado exitosamente");

            // Notificar al especialista que su trabajo fue APROBADO
            User esp = ticket.getEspecialistaAsignado();
            if (esp == null && ticket.getEspecialistaNum() != null) {
                esp = userRepository.findById(Long.valueOf(ticket.getEspecialistaNum())).orElse(null);
            }

            if (esp != null && esp.getEmail() != null) {
                log.info("Notificando APROBACIÓN al especialista: {} ({})", esp.getUsername(), esp.getEmail());
                sendStyledEmail(esp, "¡Resolución APROBADA! ✅", "El inspector ha cerrado el ticket", 
                    "¡Excelente trabajo! El inspector ha confirmado que la solución del ticket <b>" + ticket.getIdTicket() + "</b> is correcta y ha procedido a cerrarlo.", 
                    "#27ae60", ticket.getIdTicket(), "RESUELTO (CERRADO)");
            }
        } else if ("rechazar".equals(accion)) {
            ticket.setEstado("PENDIENTE");
            ticket.setAsunto(ticket.getAsunto() + "\n\n--- RECHAZO DE INSPECTOR ---\n" + motivo);
            if (evidencia != null && !evidencia.isEmpty()) {
                String filename = fileService.save(evidencia);
                String currentLink = ticket.getLinkDrive();
                if (currentLink != null && !currentLink.trim().isEmpty()) {
                    ticket.setLinkDrive(currentLink.trim() + "," + filename);
                } else {
                    ticket.setLinkDrive(filename);
                }
            }
            ticket.setTokenConfirmacion(null);
            ticket.setFechaExpiracionToken(null);
            Ticket saved = ticketRepository.save(ticket);
            logEvent(saved.getIdTicket(), prevEstado, "PENDIENTE", "Inspector rechazó la resolución. Retornado a PENDIENTE. Motivo: " + motivo);
            notifyTicketParties(saved, "ticket_actualizado");
            response.put("success", true);
            response.put("message", "Ticket devuelto al especialista");
            
            // Notificar al especialista a cargo (Infalible: vía relación directa)
            User esp = ticket.getEspecialistaAsignado();
            
            // Si por alguna razón la relación no está cargada, buscar por ID como fallback
            if (esp == null && ticket.getEspecialistaNum() != null) {
                esp = userRepository.findById(Long.valueOf(ticket.getEspecialistaNum())).orElse(null);
            }

            if (esp != null && esp.getEmail() != null) {
                log.info("Notificando rechazo al especialista A CARGO: {} ({})", esp.getUsername(), esp.getEmail());
                String linkEvidencia = "";
                if (ticket.getLinkDrive() != null && !ticket.getLinkDrive().trim().isEmpty()) {
                    StringBuilder sb = new StringBuilder("<br><br><b>Evidencias Adjuntas:</b>");
                    String[] files = ticket.getLinkDrive().split(",");
                    for (int i = 0; i < files.length; i++) {
                        String f = files[i].trim();
                        sb.append("<br>- <a href='").append(backendUrl).append("/uploads/").append(f).append("'>Ver archivo adjunto ").append(i + 1).append("</a>");
                    }
                    linkEvidencia = sb.toString();
                } else {
                    linkEvidencia = "<br><br><i>El inspector no adjuntó archivos adicionales, revise el historial en el sistema.</i>";
                }

                sendStyledEmail(esp, "Resolución RECHAZADA ❌", "El inspector no está conforme", 
                    "⚠️ Alerta: El inspector ha rechazado la solución del ticket <b>" + ticket.getIdTicket() + "</b>.<br>" +
                    "<b>Motivo:</b> " + motivo + linkEvidencia, 
                    "#e67e22", ticket.getIdTicket(), "PENDIENTE (RECHAZADO)");
            } else {
                log.error("CRÍTICO: No se pudo encontrar al especialista o su correo para el ticket {}", ticket.getIdTicket());
            }
        }

        notifyTicketParties(ticket, "ticket_actualizado");
        return response;
    }

    public void deleteTicket(String id) {
        ticketRepository.deleteById(id);
    }

    public Ticket delegateTicket(String id, Integer especialistaNum) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        String prevEstado = ticket.getEstado();
        ticket.setEspecialistaNum(especialistaNum);
        Ticket updated = ticketRepository.save(ticket);

        String espName = "Especialista ID " + especialistaNum;
        try {
            Optional<User> espOpt = userRepository.findById(Long.valueOf(especialistaNum));
            if (espOpt.isPresent()) {
                espName = espOpt.get().getUsername();
            }
        } catch (Exception e) {}

        logEvent(updated.getIdTicket(), prevEstado, updated.getEstado(), "Administrador delegó el ticket al especialista: " + espName);
        notifyTicketParties(updated, "ticket_actualizado");
        
        // Enviar correo al nuevo especialista
        Optional<User> espOpt = userRepository.findById(Long.valueOf(especialistaNum));
        if (espOpt.isEmpty()) {
            espOpt = userRepository.findByEspecialistaNum(especialistaNum);
        }

        espOpt.ifPresent(esp -> {
            sendStyledEmail(esp, "Ticket Delegado", "Se le ha asignado una nueva solicitud", 
                "Se le ha delegado un ticket para su atención.<br><b>Asunto:</b> " + updated.getAsunto() + "<br>Por favor, ingrese al sistema para revisar los detalles.", 
                "#6366f1", updated.getIdTicket(), updated.getEstado());
        });
        
        return updated;
    }

    public List<User> getEspecialistasByIre(String codIre) {
        // En BD el rol se guarda como "ESPECIALISTA", sin prefijo ROLE_
        return userRepository.findByCodIreAndRole_NombreRol(codIre, "ESPECIALISTA").stream()
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .collect(java.util.stream.Collectors.toList());
    }

    public Map<String, Long> getEstadisticas(User user) {
        List<Ticket> tickets;
        
        // Determinar qué tickets debe ver el usuario para sus estadísticas
        String role = (user.getRole() != null && user.getRole().getNombreRol() != null) 
                      ? user.getRole().getNombreRol().toUpperCase() : "";
        
        if (role.contains("ADMIN")) {
            tickets = ticketRepository.findAll();
        } else if (role.equals("ESPECIALISTA")) {
            tickets = ticketRepository.findByEspecialistaNum(user.getIdUsuario().intValue());
        } else {
            tickets = ticketRepository.findByCreador(user);
        }

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) tickets.size());
        
        // Contamos como PENDIENTES tanto el estado base como los que esperan confirmación y los recién creados
        stats.put("pendientes", tickets.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("PENDIENTE") || 
             t.getEstado().equalsIgnoreCase("PENDIENTE_CONFIRMACION") ||
             t.getEstado().equalsIgnoreCase("CREADO"))).count());
            
        stats.put("en_proceso", tickets.stream().filter(t -> t.getEstado() != null && 
            t.getEstado().equalsIgnoreCase("EN_PROCESO")).count());
            
        stats.put("resueltos", tickets.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("RESUELTO") || t.getEstado().equalsIgnoreCase("CERRADO"))).count());
            
        stats.put("rechazados", tickets.stream().filter(t -> t.getEstado() != null && 
            t.getEstado().equalsIgnoreCase("RECHAZADO")).count());
            
        return stats;
    }

    /**
     * Notifica a todas las partes involucradas en un ticket (creador + especialista + admins).
     */
    private void notifyTicketParties(Ticket ticket, String eventName) {
        Set<Long> targetUsers = new HashSet<>();
        if (ticket.getCreador() != null) targetUsers.add(ticket.getCreador().getIdUsuario());
        if (ticket.getEspecialistaNum() != null) targetUsers.add(Long.valueOf(ticket.getEspecialistaNum()));
        notificationService.dispatchEventToUsers(targetUsers, eventName, ticket);
        // Broadcast para admins
        notificationService.dispatchEvent(eventName, ticket);
    }

    public List<TicketLog> getTicketHistorial(String idTicket) {
        return ticketLogRepository.findByIdTicketOrderByFechaAsc(idTicket);
    }

    private void logEvent(String idTicket, String prevStatus, String newStatus, String details) {
        String currentUser = "SYSTEM";
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                currentUser = auth.getName();
            }
        } catch (Exception e) {
            log.error("Error al obtener usuario para auditoría", e);
        }
        TicketLog ticketLog = new TicketLog(idTicket, prevStatus, newStatus, currentUser, details);
        ticketLogRepository.save(ticketLog);
    }
}
