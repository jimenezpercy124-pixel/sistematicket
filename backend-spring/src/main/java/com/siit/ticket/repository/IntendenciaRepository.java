package com.siit.ticket.repository;

import com.siit.ticket.model.Intendencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntendenciaRepository extends JpaRepository<Intendencia, String> {
}
