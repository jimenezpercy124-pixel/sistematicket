package com.siit.ticket.scratch;

import com.siit.ticket.model.User;
import com.siit.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserListScratch implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DEBUG: Listing all users and their details...");
        userRepository.findAll().forEach(user -> {
            System.out.println("User: " + user.getUsername() + 
                ", ID: " + user.getIdUsuario() + 
                ", Rank (espNum): " + user.getEspecialistaNum() + 
                ", Role: " + (user.getRole() != null ? user.getRole().getNombreRol() : "NONE"));
        });
    }
}
