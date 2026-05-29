package com.siit.ticket.repository;

import com.siit.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    java.util.List<User> findByCodIreAndRole_NombreRol(String codIre, String roleName);
    java.util.List<User> findByRole_NombreRol(String roleName);
    Optional<User> findByEspecialistaNum(Integer especialistaNum);
}
