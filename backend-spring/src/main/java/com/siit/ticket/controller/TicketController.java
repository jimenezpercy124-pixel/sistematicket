package com.siit.ticket.controller;

import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.TicketLog;
import com.siit.ticket.model.User;
import com.siit.ticket.repository.UserRepository;
import com.siit.ticket.repository.TicketRepository;
import com.siit.ticket.service.TicketService;
import com.siit.ticket.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.file.Path;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FileService fileService;

    // ─── Obtener usuario autenticado desde el token JWT ───────────────────────
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
    }

    // ─── INSPECTOR: Ver sus propios tickets ───────────────────────────────────
    @GetMapping("/tickets/mis-tickets")
    public List<Ticket> getMisTickets() {
        User currentUser = getCurrentUser();
        return ticketService.getTicketsByCreador(currentUser);
    }

    // ─── INSPECTOR/ESPECIALISTA: Ver tickets de una IRE ───────────────────────
    @GetMapping("/tickets/ire/{codIre}")
    public List<Ticket> getTicketsByIre(@PathVariable String codIre) {
        return ticketService.getTicketsByIre(codIre);
    }

    @GetMapping("/tickets/especialista/{num}")
    public List<Ticket> getTicketsByEspecialista(@PathVariable Long num) {
        return ticketService.getTicketsByEspecialista(num.intValue());
    }

    @GetMapping("/tickets/especialistas-disponibles")
    public List<User> getEspecialistasDisponibles() {
        // En BD el rol se guarda como "ESPECIALISTA", sin prefijo ROLE_
        return userRepository.findByRole_NombreRol("ESPECIALISTA").stream()
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── INSPECTOR: Crear ticket ──────────────────────────────────────────────
    @PostMapping(value = "/tickets/crear", consumes = "multipart/form-data")
    public ResponseEntity<?> createTicket(
            @RequestParam("asunto") String asunto,
            @RequestParam(value = "codIre",    required = false) String codIre,
            @RequestParam(value = "cod_ire",   required = false) String codIreAlt,
            @RequestParam(value = "id_incidencia", required = false) Long idIncidencia,
            @RequestParam(value = "evidencia", required = false) MultipartFile evidencia) {

        User currentUser = getCurrentUser();

        // Aceptar ambos formatos: camelCase y snake_case
        String ire = (codIre != null) ? codIre : (codIreAlt != null ? codIreAlt : currentUser.getCodIre());

        Ticket ticket = new Ticket();
        ticket.setAsunto(asunto);
        ticket.setCodIre(ire);
        ticket.setCreador(currentUser);

        // Si se seleccionó incidencia, buscar y asignar el especialista de esa incidencia
        if (idIncidencia != null) {
            ticketService.asignarEspecialistaPorIncidencia(ticket, idIncidencia);
        }

        Ticket saved = ticketService.createTicketWithFile(ticket, evidencia);
        return ResponseEntity.ok(Map.of(
            "id_ticket", saved.getIdTicket(),
            "estado", saved.getEstado(),
            "mensaje", "Ticket generado correctamente"
        ));
    }

    // ─── ESPECIALISTA/ADMIN: Actualizar estado de un ticket ───────────────────
    @PatchMapping("/tickets/{id}/status")
    public Ticket updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        String motivo = body.get("motivo");
        return ticketService.updateStatus(id, estado, motivo);
    }

    // ─── ADMIN: Delegar ticket a especialista ─────────────────────────────────
    @PatchMapping("/tickets/{id}/delegate")
    public Ticket delegateTicket(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        return ticketService.delegateTicket(id, body.get("especialista_num"));
    }

    // ─── INSPECTOR: Confirmar resolución ─────────────────────────────────────
    @GetMapping("/tickets/confirmar")
    public ResponseEntity<?> confirmar(@RequestParam String token, @RequestParam String accion) {
        return ResponseEntity.ok(ticketService.processConfirmation(token, accion, null, null));
    }

    // ─── INSPECTOR: Rechazar resolución con motivo y evidencia ────────────────
    @PostMapping(value = "/tickets/rechazar", consumes = "multipart/form-data")
    public ResponseEntity<?> rechazar(
            @RequestParam("token") String token,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "evidencia", required = false) MultipartFile evidencia) {
        return ResponseEntity.ok(ticketService.processConfirmation(token, "rechazar", motivo, evidencia));
    }

    // ─── INSPECTOR: Confirmar/Rechazar directamente desde el sistema (sin token) ──
    @PatchMapping("/tickets/{id}/confirmar-directo")
    public ResponseEntity<?> confirmarDirecto(@PathVariable String id) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ticketService.processDirectConfirmation(id, "confirmar", null, null, currentUser));
    }

    @PostMapping(value = "/tickets/{id}/rechazar-directo", consumes = "multipart/form-data")
    public ResponseEntity<?> rechazarDirecto(
            @PathVariable String id,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "evidencia", required = false) MultipartFile evidencia) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ticketService.processDirectConfirmation(id, "rechazar", motivo, evidencia, currentUser));
    }

    @GetMapping("/tickets/stats")
    public Map<String, Long> getStats() {
        return ticketService.getEstadisticas(getCurrentUser());
    }

    @GetMapping("/especialistas/ire/{codIre}")
    public List<User> getEspecialistasByIre(@PathVariable String codIre) {
        return ticketService.getEspecialistasByIre(codIre);
    }

    @GetMapping("/tickets/{id}/historial")
    public List<TicketLog> getTicketHistorial(@PathVariable String id) {
        return ticketService.getTicketHistorial(id);
    }

    // ─── Descarga segura y controlada de archivos de evidencia ────────────────
    @GetMapping("/tickets/files/{filename}")
    public ResponseEntity<Resource> getTicketFile(@PathVariable String filename) {
        try {
            User currentUser = getCurrentUser();
            String role = (currentUser.getRole() != null && currentUser.getRole().getNombreRol() != null)
                    ? currentUser.getRole().getNombreRol().toUpperCase() : "";

            // Buscar el ticket por el archivo de evidencia en linkDrive de manera eficiente
            Ticket ticket = ticketRepository.findByLinkDriveContaining(filename)
                    .orElseThrow(() -> new RuntimeException("Archivo no asociado a ningún ticket válido."));

            // Validar acceso:
            // - ADMIN: Acceso total.
            // - ESPECIALISTA: Acceso si está asignado o es de su IRE.
            // - INSPECTOR / OTRO: Acceso solo si es el creador del ticket.
            boolean isAuthorized = false;
            if (role.contains("ADMIN")) {
                isAuthorized = true;
            } else if (role.equals("ESPECIALISTA")) {
                boolean isAssigned = ticket.getEspecialistaNum() != null && ticket.getEspecialistaNum().equals(currentUser.getIdUsuario().intValue());
                boolean isSameIre = ticket.getCodIre() != null && ticket.getCodIre().equalsIgnoreCase(currentUser.getCodIre());
                if (isAssigned || isSameIre) {
                    isAuthorized = true;
                }
            } else {
                if (ticket.getCreador() != null && ticket.getCreador().getIdUsuario().equals(currentUser.getIdUsuario())) {
                    isAuthorized = true;
                }
            }

            if (!isAuthorized) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path filePath = fileService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = null;
                try {
                    contentType = java.nio.file.Files.probeContentType(filePath);
                } catch (Exception ignored) {}
                
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
