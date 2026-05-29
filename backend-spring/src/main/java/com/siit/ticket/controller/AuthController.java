package com.siit.ticket.controller;

import com.siit.ticket.dto.ActualizarPasswordRequest;
import com.siit.ticket.dto.JwtResponse;
import com.siit.ticket.dto.LoginRequest;
import com.siit.ticket.model.User;
import com.siit.ticket.repository.UserRepository;
import com.siit.ticket.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.siit.ticket.service.LdapService;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LdapService ldapService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginRequest.getUsername(), loginRequest.getUsername());

        if (userOpt.isPresent() && Boolean.FALSE.equals(userOpt.get().getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario inactivo o inhabilitado");
        }

        if (ldapService.isLdapEnabled()) {
            boolean isAuthenticated = ldapService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
            if (!isAuthenticated) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales de red incorrectas");
            }
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no registrado en el sistema local");
            }
        } else {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
                );
            } catch (BadCredentialsException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
            } catch (org.springframework.security.authentication.DisabledException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario inactivo o inhabilitado");
            }
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String rol = user.getRole() != null ? user.getRole().getNombreRol().trim().toUpperCase() : "USUARIO";
            
            final String jwt = jwtUtil.generateToken(user.getUsername(), rol, user.getIdUsuario(), user.getCodIre());

            return ResponseEntity.ok(new JwtResponse(
                jwt, 
                user.getIdUsuario(), 
                user.getUsername(), 
                rol, 
                user.getCodIre(), 
                user.getEspecialistaNum(),
                !ldapService.isLdapEnabled() && user.getDebeCambiarPassword() != null && user.getDebeCambiarPassword()
            ));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al generar token");
    }

    @PostMapping("/actualizar-password")
    public ResponseEntity<?> actualizarPassword(@RequestBody ActualizarPasswordRequest solicitud) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sesión no válida o expirada.");
            }
            
            String username = auth.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User usuario = userOpt.get();
                usuario.setPassword(passwordEncoder.encode(solicitud.getNuevaPassword()));
                usuario.setDebeCambiarPassword(false);
                userRepository.save(usuario);
                return ResponseEntity.ok(java.util.Map.of("message", "Contraseña actualizada con éxito"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error detalle: " + e.getMessage());
        }
    }
}
