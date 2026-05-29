package com.siit.ticket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    public void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        
        // Inyectar propiedades manualmente mediante reflexión para pruebas rápidas unitarias
        java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "SIITSunafilSecretKey2026_MuyLargaParaHS256_AtLeast32Chars!!");

        java.lang.reflect.Field expField = JwtUtil.class.getDeclaredField("expirationHours");
        expField.setAccessible(true);
        expField.set(jwtUtil, 10);

        jwtUtil.init();
    }

    @Test
    public void testGenerateAndExtractToken() {
        String username = "percy";
        String role = "INSPECTOR";
        Long id = 123L;
        String codIre = "APURIMAC";

        String token = jwtUtil.generateToken(username, role, id, codIre);
        assertNotNull(token);

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);

        String extractedRole = jwtUtil.extractClaim(token, claims -> claims.get("rol", String.class));
        assertEquals(role, extractedRole);
        
        Long extractedId = jwtUtil.extractClaim(token, claims -> claims.get("id", Long.class));
        assertEquals(id, extractedId);
    }

    @Test
    public void testValidateTokenSuccess() {
        String username = "percy";
        String token = jwtUtil.generateToken(username, "INSPECTOR", 123L, "APURIMAC");
        UserDetails userDetails = new User(username, "password", true, true, true, true, new ArrayList<>());

        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    public void testValidateTokenFailureForDifferentUser() {
        String token = jwtUtil.generateToken("percy", "INSPECTOR", 123L, "APURIMAC");
        UserDetails userDetails = new User("otroUsuario", "password", true, true, true, true, new ArrayList<>());

        assertFalse(jwtUtil.validateToken(token, userDetails));
    }
}
