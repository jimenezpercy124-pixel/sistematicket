package com.siit.ticket.repository;

import com.siit.ticket.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    boolean existsByEspecialistaNum(Integer especialistaNum);
}
