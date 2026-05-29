package com.siit.ticket.repository;

import com.siit.ticket.model.TicketSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketSequenceRepository extends JpaRepository<TicketSequence, String> {
}
