package com.siit.ticket.controller;

import com.siit.ticket.model.Incidencia;
import com.siit.ticket.model.Intendencia;
import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.User;
import com.siit.ticket.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // --- USERS ---
    @GetMapping("/usuarios")
    public List<User> getUsuarios() {
        return adminService.getAllUsuarios();
    }

    @PostMapping("/crear-usuario")
    public User createUsuario(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setUsername(body.get("username").toString());
        user.setEmail(body.get("email") != null ? body.get("email").toString() : null);
        user.setCodIre(body.get("cod_ire") != null ? body.get("cod_ire").toString() : null);
        
        if (body.get("especialista_num") != null) {
            user.setEspecialistaNum(Integer.valueOf(body.get("especialista_num").toString()));
        }

        String pass = body.get("password") != null ? body.get("password").toString() : "Sunafil2026!";
        user.setPassword(pass);

        String roleName = body.get("rol").toString();
        return adminService.createUsuario(user, roleName);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> deleteUsuario(@PathVariable Long id) {
        try {
            adminService.deleteUsuario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/usuarios/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String newPass = body.get("password").toString();
        adminService.resetPassword(id, newPass);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Boolean active = (Boolean) body.get("activo");
            adminService.toggleUserStatus(id, active);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- CATALOGS ---
    @GetMapping("/incidencias")
    public List<Incidencia> getIncidencias() {
        return adminService.getAllIncidencias();
    }

    @PostMapping("/incidencias")
    public Incidencia createIncidencia(@RequestBody Incidencia incidencia) {
        return adminService.createIncidencia(incidencia);
    }

    @DeleteMapping("/incidencias/{id}")
    public ResponseEntity<?> deleteIncidencia(@PathVariable Long id) {
        try {
            adminService.deleteIncidencia(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/intendencias")
    public List<Intendencia> getIntendencias() {
        return adminService.getAllIntendencias();
    }

    // --- TICKETS ADMIN ---
    @GetMapping("/todos-los-tickets")
    public List<Ticket> getTodosLosTickets() {
        return adminService.getAllTickets();
    }

    @Autowired
    private com.siit.ticket.repository.UserRepository userRepository;

    @Autowired
    private com.siit.ticket.service.TicketService ticketService;

    @GetMapping("/estadisticas")
    public Map<String, Long> getEstadisticas() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ticketService.getEstadisticas(currentUser);
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable String id) {
        try {
            adminService.deleteTicket(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- REPORTS ---
    @GetMapping("/reportes/ires")
    public List<Map<String, Object>> getReporteIres() {
        return adminService.getReporteIres();
    }

    @GetMapping("/reportes/tematicas")
    public List<Map<String, Object>> getReporteTematicas() {
        return adminService.getReporteTematicas();
    }

    @GetMapping("/reportes")
    public Map<String, Object> getReportes() {
        Map<String, Object> reportes = new HashMap<>();
        reportes.put("ires", adminService.getReporteIres());
        reportes.put("tematicas", adminService.getReporteTematicas());
        reportes.put("cruce", adminService.getReporteCruce());
        return reportes;
    }

    // --- CONTINGENCIA ---
    @PatchMapping("/incidencias/{id}/reassign-specialist")
    public ResponseEntity<?> reassignSpecialist(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Integer newEspecialistaNum = Integer.valueOf(body.get("id_especialista").toString());
            Incidencia updated = adminService.reasignarEspecialistaIncidencia(id, newEspecialistaNum);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/contingencia/activar")
    public ResponseEntity<?> activarContingencia(@RequestBody Map<String, Object> body) {
        try {
            Integer idAusente = Integer.valueOf(body.get("id_ausente").toString());
            Integer idReemplazo = Integer.valueOf(body.get("id_reemplazo").toString());
            Map<String, Object> res = adminService.activarPlanContingencia(idAusente, idReemplazo);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
