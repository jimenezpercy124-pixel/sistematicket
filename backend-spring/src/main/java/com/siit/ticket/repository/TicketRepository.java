package com.siit.ticket.repository;

import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByCodIre(String codIre);
    List<Ticket> findByCreador(User user);
    List<Ticket> findByEspecialistaNum(Integer especialistaNum);
    java.util.Optional<Ticket> findByTokenConfirmacion(String token);
    java.util.Optional<Ticket> findByLinkDriveContaining(String filename);
    boolean existsByCreadorIdUsuario(Long idUsuario);
    boolean existsByEspecialistaNum(Integer especialistaNum);
}
