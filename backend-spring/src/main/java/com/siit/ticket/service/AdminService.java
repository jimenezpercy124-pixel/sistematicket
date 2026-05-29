package com.siit.ticket.service;

import com.siit.ticket.model.Incidencia;
import com.siit.ticket.model.Intendencia;
import com.siit.ticket.model.User;
import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.Role;
import com.siit.ticket.repository.IncidenciaRepository;
import com.siit.ticket.repository.IntendenciaRepository;
import com.siit.ticket.repository.RoleRepository;
import com.siit.ticket.repository.TicketRepository;
import com.siit.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private IntendenciaRepository intendenciaRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    // --- USERS ---
    public List<User> getAllUsuarios() {
        return userRepository.findAll();
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Map<String, Object> getEstadisticas() {
        List<Ticket> all = ticketRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("pendientes", all.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("PENDIENTE") || 
             t.getEstado().equalsIgnoreCase("PENDIENTE_CONFIRMACION") ||
             t.getEstado().equalsIgnoreCase("CREADO"))).count());
        stats.put("en_proceso", all.stream().filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("EN_PROCESO")).count());
        stats.put("resueltos", all.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("RESUELTO") || t.getEstado().equalsIgnoreCase("CERRADO"))).count());
        stats.put("rechazados", all.stream().filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("RECHAZADO")).count());
        return stats;
    }

    // Alias para el AdminController
    public Map<String, Long> getEstadisticasTickets() {
        List<Ticket> all = ticketRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) all.size());
        stats.put("pendientes", all.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("PENDIENTE") || 
             t.getEstado().equalsIgnoreCase("PENDIENTE_CONFIRMACION") ||
             t.getEstado().equalsIgnoreCase("CREADO"))).count());
        stats.put("en_proceso", all.stream().filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("EN_PROCESO")).count());
        stats.put("resueltos", all.stream().filter(t -> t.getEstado() != null && 
            (t.getEstado().equalsIgnoreCase("RESUELTO") || t.getEstado().equalsIgnoreCase("CERRADO"))).count());
        stats.put("rechazados", all.stream().filter(t -> t.getEstado() != null && t.getEstado().equalsIgnoreCase("RECHAZADO")).count());
        return stats;
    }

    public void deleteTicket(String id) {
        ticketRepository.deleteById(id);
    }

    public User createUsuario(User user, String roleName) {
        Role role = roleRepository.findByNombreRol(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDebeCambiarPassword(!"ADMIN".equalsIgnoreCase(roleName));
        return userRepository.save(user);
    }

    public void deleteUsuario(Long id) {
        // 1. Verificar si es creador de algún ticket
        if (ticketRepository.existsByCreadorIdUsuario(id)) {
            throw new RuntimeException("No se puede eliminar el usuario porque tiene tickets creados.");
        }
        
        // 2. Verificar si está asignado como especialista en algún ticket
        // Nota: especialistaNum en Ticket guarda el ID del usuario
        if (ticketRepository.existsByEspecialistaNum(id.intValue())) {
            throw new RuntimeException("No se puede eliminar el usuario porque tiene tickets asignados como especialista.");
        }

        // 3. Verificar si está asignado como especialista por defecto en alguna categoría de incidencia
        if (incidenciaRepository.existsByEspecialistaNum(id.intValue())) {
             throw new RuntimeException("No se puede eliminar el usuario porque es el especialista asignado a una categoría de incidencias.");
        }

        userRepository.deleteById(id);
    }


    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        String roleName = (user.getRole() != null) ? user.getRole().getNombreRol() : "";
        user.setDebeCambiarPassword(!"ADMIN".equalsIgnoreCase(roleName));
        userRepository.save(user);
    }

    // --- CATALOGS ---
    public List<Incidencia> getAllIncidencias() {
        return incidenciaRepository.findAll();
    }

    public Incidencia createIncidencia(Incidencia incidencia) {
        if (incidencia.getEspecialistaNum() != null) {
            User esp = userRepository.findById(Long.valueOf(incidencia.getEspecialistaNum()))
                .orElseThrow(() -> new RuntimeException("Especialista no encontrado"));
            if (Boolean.FALSE.equals(esp.getActivo())) {
                throw new RuntimeException("El especialista seleccionado está inhabilitado.");
            }
        }
        return incidenciaRepository.save(incidencia);
    }

    public void deleteIncidencia(Long id) {
        incidenciaRepository.deleteById(id);
    }

    public List<Intendencia> getAllIntendencias() {
        return intendenciaRepository.findAll();
    }

    public List<Map<String, Object>> getReporteIres() {
        List<Intendencia> ires = intendenciaRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        
        return ires.stream().map(ire -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre_ire", ire.getNombreIre());
            long total = allTickets.stream().filter(t -> ire.getCodIre().equals(t.getCodIre())).count();
            long resueltos = allTickets.stream().filter(t -> ire.getCodIre().equals(t.getCodIre()) && "RESUELTO".equalsIgnoreCase(t.getEstado())).count();
            long enProceso = allTickets.stream().filter(t -> ire.getCodIre().equals(t.getCodIre()) && "EN_PROCESO".equalsIgnoreCase(t.getEstado())).count();
            long pendientes = allTickets.stream().filter(t -> ire.getCodIre().equals(t.getCodIre()) && "PENDIENTE".equalsIgnoreCase(t.getEstado())).count();
            
            map.put("total_tickets", total);
            map.put("resueltos", resueltos);
            map.put("en_proceso", enProceso);
            map.put("pendientes", pendientes);
            return map;
        }).filter(map -> (Long) map.get("total_tickets") > 0)
          .sorted((m1, m2) -> Long.compare((Long) m2.get("total_tickets"), (Long) m1.get("total_tickets")))
          .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getReporteTematicas() {
        List<Incidencia> incs = incidenciaRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();

        return incs.stream().map(inc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre_incidencia", inc.getNombreIncidencia());
            
            // Filtrar tickets que tienen esta incidencia asignada
            List<Ticket> filtered = allTickets.stream()
                .filter(t -> t.getIncidencia() != null && t.getIncidencia().getIdIncidencia().equals(inc.getIdIncidencia()))
                .collect(Collectors.toList());
                
            long total = filtered.size();
            long resueltos = filtered.stream().filter(t -> "RESUELTO".equals(t.getEstado())).count();
            long enProceso = filtered.stream().filter(t -> "EN_PROCESO".equals(t.getEstado())).count();
            long pendientes = filtered.stream().filter(t -> "PENDIENTE".equals(t.getEstado())).count();

            map.put("total", total);
            map.put("resueltos", resueltos);
            map.put("en_proceso", enProceso);
            map.put("pendientes", pendientes);
            return map;
        }).filter(map -> ((Number) map.get("total")).longValue() > 0)
          .sorted((m1, m2) -> Long.compare(((Number) m2.get("total")).longValue(), ((Number) m1.get("total")).longValue()))
          .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getReporteCruce() {
        List<Intendencia> ires = intendenciaRepository.findAll();
        List<Incidencia> incs = incidenciaRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        
        return ires.stream().map(ire -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre_ire", ire.getNombreIre());
            
            List<Ticket> ireTickets = allTickets.stream()
                .filter(t -> ire.getCodIre().equals(t.getCodIre()))
                .collect(Collectors.toList());
            map.put("total_tickets", ireTickets.size());
            
            List<Map<String, Object>> incidenciasList = incs.stream().map(inc -> {
                long count = ireTickets.stream()
                    .filter(t -> t.getIncidencia() != null && t.getIncidencia().getIdIncidencia().equals(inc.getIdIncidencia()))
                    .count();
                if (count > 0) {
                    Map<String, Object> incMap = new HashMap<>();
                    incMap.put("nombre_incidencia", inc.getNombreIncidencia());
                    incMap.put("total", count);
                    return incMap;
                }
                return null;
            }).filter(Objects::nonNull)
              .sorted((m1, m2) -> Long.compare((Long) m2.get("total"), (Long) m1.get("total")))
              .collect(Collectors.toList());
            
            map.put("incidencias", incidenciasList);
            return map;
        }).filter(map -> ((Number) map.get("total_tickets")).intValue() > 0)
          .sorted((m1, m2) -> Integer.compare(((Number) m2.get("total_tickets")).intValue(), ((Number) m1.get("total_tickets")).intValue()))
          .collect(Collectors.toList());
    }

    // --- CONTINGENCIA ---
    @Transactional
    public Map<String, Object> activarPlanContingencia(Integer idAusente, Integer idReemplazo) {
        User ausente = userRepository.findById(Long.valueOf(idAusente))
                .orElseThrow(() -> new RuntimeException("Especialista ausente no encontrado"));
        User reemplazo = userRepository.findById(Long.valueOf(idReemplazo))
                .orElseThrow(() -> new RuntimeException("Especialista de reemplazo no encontrado"));

        if (Boolean.FALSE.equals(reemplazo.getActivo())) {
            throw new RuntimeException("El especialista de reemplazo seleccionado está inhabilitado.");
        }

        // 1. Reasignar todas las incidencias del ausente al reemplazo
        List<Incidencia> incidencias = incidenciaRepository.findAll();
        List<Incidencia> incidenciasModificadas = new ArrayList<>();
        for (Incidencia inc : incidencias) {
            if (inc.getEspecialistaNum() != null && inc.getEspecialistaNum().equals(idAusente)) {
                inc.setEspecialistaNum(idReemplazo);
                incidenciaRepository.save(inc);
                incidenciasModificadas.add(inc);
            }
        }

        // 2. Migrar carga activa de tickets del ausente al reemplazo
        List<Ticket> ticketsModificados = new ArrayList<>();
        List<Ticket> ticketsDelAusente = ticketRepository.findByEspecialistaNum(idAusente);
        for (Ticket t : ticketsDelAusente) {
            String estado = t.getEstado() != null ? t.getEstado().toUpperCase() : "";
            if (estado.equals("PENDIENTE") || estado.equals("ABIERTO") || estado.equals("EN_PROCESO") || estado.equals("CREADO") || estado.equals("RECHAZADO")) {
                t.setEspecialistaNum(idReemplazo);
                Ticket saved = ticketRepository.save(t);
                ticketsModificados.add(saved);
                
                // Notificar vía websockets/SSE
                notifyTicketParties(saved, "ticket_actualizado");
                
                // Enviar correo informativo
                sendContingencyEmail(reemplazo, saved, ausente.getUsername());
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", true);
        resultado.put("incidencias_reasignadas", incidenciasModificadas.size());
        resultado.put("tickets_migrados", ticketsModificados.size());
        return resultado;
    }

    @Transactional
    public Incidencia reasignarEspecialistaIncidencia(Long idIncidencia, Integer newEspecialistaNum) {
        Incidencia incidencia = incidenciaRepository.findById(idIncidencia)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));
        
        Integer oldEspecialistaNum = incidencia.getEspecialistaNum();
        incidencia.setEspecialistaNum(newEspecialistaNum);
        Incidencia saved = incidenciaRepository.save(incidencia);
        
        // Si hay especialista anterior y es diferente al nuevo, migrar tickets activos de esta incidencia
        if (oldEspecialistaNum != null && !oldEspecialistaNum.equals(newEspecialistaNum)) {
            User reemplazo = userRepository.findById(Long.valueOf(newEspecialistaNum))
                    .orElseThrow(() -> new RuntimeException("Especialista no encontrado"));

            if (Boolean.FALSE.equals(reemplazo.getActivo())) {
                throw new RuntimeException("El especialista seleccionado está inhabilitado.");
            }
            
            List<Ticket> tickets = ticketRepository.findByEspecialistaNum(oldEspecialistaNum);
            for (Ticket t : tickets) {
                if (t.getIncidencia() != null && t.getIncidencia().getIdIncidencia().equals(idIncidencia)) {
                    String estado = t.getEstado() != null ? t.getEstado().toUpperCase() : "";
                    if (estado.equals("PENDIENTE") || estado.equals("ABIERTO") || estado.equals("EN_PROCESO") || estado.equals("CREADO") || estado.equals("RECHAZADO")) {
                        t.setEspecialistaNum(newEspecialistaNum);
                        Ticket savedTicket = ticketRepository.save(t);
                        notifyTicketParties(savedTicket, "ticket_actualizado");
                        sendContingencyEmail(reemplazo, savedTicket, "reasignación de categoría");
                    }
                }
            }
        }
        return saved;
    }

    private void notifyTicketParties(Ticket ticket, String eventName) {
        java.util.Set<Long> targetUsers = new java.util.HashSet<>();
        if (ticket.getCreador() != null) targetUsers.add(ticket.getCreador().getIdUsuario());
        if (ticket.getEspecialistaNum() != null) targetUsers.add(Long.valueOf(ticket.getEspecialistaNum()));
        notificationService.dispatchEventToUsers(targetUsers, eventName, ticket);
        notificationService.dispatchEvent(eventName, ticket);
    }

    private void sendContingencyEmail(User user, Ticket ticket, String motivoAusencia) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            return;
        }
        String body = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
                    + "  <div style='background-color: #e67e22; color: white; padding: 25px; text-align: center;'>"
                    + "    <h2 style='margin: 0; font-size: 24px;'>Plan de Contingencia Activado</h2>"
                    + "    <p style='margin: 5px 0 0; opacity: 0.8; font-size: 14px;'>Asignación por Ausencia de Técnico</p>"
                    + "  </div>"
                    + "  <div style='padding: 30px; color: #2d3436; line-height: 1.6;'>"
                    + "    <h3 style='color: #e67e22; margin-top: 0;'>¡Hola, " + user.getUsername() + "!</h3>"
                    + "    <p style='font-size: 15px;'>Se ha activado el plan de contingencia debido a la ausencia de <b>" + motivoAusencia + "</b> (o reasignación de categoría). Se te ha asignado el siguiente ticket activo:</p>"
                    + "    <table style='width: 100%; background: #f8f9fa; padding: 20px; border-radius: 12px; margin: 20px 0; border-collapse: collapse;'>"
                    + "      <tr><td style='padding: 8px 0;'><b>ID Ticket:</b></td><td style='color: #e74c3c; font-weight: bold;'>" + ticket.getIdTicket() + "</td></tr>"
                    + "      <tr><td style='padding: 8px 0;'><b>Asunto:</b></td><td>" + ticket.getAsunto() + "</td></tr>"
                    + "      <tr><td style='padding: 8px 0;'><b>Estado:</b></td><td><span style='background: #e67e22; color: white; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: bold;'>" + ticket.getEstado() + "</span></td></tr>"
                    + "    </table>"
                    + "    <p style='font-size: 13px; color: #636e72; text-align: center; margin-top: 25px; border-top: 1px solid #eee; padding-top: 20px;'><b>Sistema de Ticket - SIIT - SUNAFIL</b><br>"
                    + "    <i style='font-size: 11px; opacity: 0.8;'>Este es un mensaje automático, por favor no responda a este correo.</i></p>"
                    + "  </div>"
                    + "</div>";
        try {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                emailService.sendEmail(user.getEmail(), "SIIT - Contingencia Asignada: " + ticket.getIdTicket(), body);
            });
        } catch (Exception e) {
            // Ignorar errores menores de email
        }
    }

    @Transactional
    public void toggleUserStatus(Long id, Boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setActivo(active);
        userRepository.save(user);
    }
}
