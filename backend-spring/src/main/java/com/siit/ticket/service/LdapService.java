package com.siit.ticket.service;

import com.siit.ticket.model.User;
import com.siit.ticket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LdapService {

    private static final Logger log = LoggerFactory.getLogger(LdapService.class);

    @Value("${app.auth.provider:database}")
    private String authProvider;

    @Value("${ldap.urls:ldap://localhost:389}")
    private String ldapUrls;

    @Value("${ldap.base-dn:dc=sunafil,dc=gob,dc=pe}")
    private String ldapBaseDn;

    @Autowired
    private UserRepository userRepository;

    /**
     * Autentica contra el Directorio Activo (LDAP) de SUNAFIL.
     * Si está en modo "database", retorna falso.
     * En entorno de pruebas/desarrollo local, simula la conexión con éxito si el usuario existe.
     * 
     * @param username Nombre de usuario (DNI o cuenta corporativa)
     * @param password Contraseña de red
     * @return true si la autenticación es exitosa
     */
    public boolean authenticate(String username, String password) {
        if (!"ldap".equalsIgnoreCase(authProvider)) {
            return false;
        }

        log.info("Iniciando autenticación LDAP para usuario [{}] contra {}", username, ldapUrls);
        
        // Simulación segura para desarrollo local / pruebas
        if (ldapUrls.contains("localhost") || ldapUrls.contains("127.0.0.1")) {
            log.warn("LDAP está configurado en localhost. Usando MOCK LDAP (simulación).");
            
            // Simular autenticación exitosa si el usuario existe y la contraseña no está vacía
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username);
            if (userOpt.isPresent()) {
                boolean matches = password != null && !password.trim().isEmpty();
                if (matches) {
                    log.info("LDAP MOCK: Autenticación exitosa para el usuario {}", username);
                    return true;
                }
            }
            log.warn("LDAP MOCK: Usuario no encontrado o contraseña vacía.");
            return false;
        }

        // Bloque de producción real usando JNDI Javax Naming
        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(javax.naming.Context.PROVIDER_URL, ldapUrls);
            env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
            
            // Formatear el DN del usuario (según el esquema de SUNAFIL, ej: uid=usuario,ou=usuarios,dc=sunafil,dc=gob,dc=pe)
            String principal = "uid=" + username + "," + ldapBaseDn;
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, principal);
            env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
            
            new javax.naming.directory.InitialDirContext(env).close();
            log.info("LDAP: Autenticación exitosa en el servidor real para usuario {}", username);
            return true;
        } catch (Exception e) {
            log.error("LDAP: Error al autenticar contra el servidor LDAP real: {}", e.getMessage());
            return false;
        }
    }

    public boolean isLdapEnabled() {
        return "ldap".equalsIgnoreCase(authProvider);
    }
}
