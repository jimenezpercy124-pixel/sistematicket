package com.siit.ticket.security;

import com.siit.ticket.model.User;
import com.siit.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Buscamos al usuario por su username O por su email
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o correo no encontrado: " + usernameOrEmail));

        String roleName = (user.getRole() != null) ? user.getRole().getNombreRol().trim().toUpperCase() : "INSPECTOR";
        
        // Log para depuración (comentado para evitar spam en consola)
        // System.out.println("DEBUG: Cargando usuario [" + username + "] con rol: [ROLE_" + roleName + "]");

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                !Boolean.FALSE.equals(user.getActivo()), // enabled
                true,
                true,
                true,
                Collections.singletonList(authority)
        );
    }
}
