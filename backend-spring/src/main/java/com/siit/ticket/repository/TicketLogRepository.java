package com.siit.ticket.repository;

import com.siit.ticket.model.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByIdTicketOrderByFechaAsc(String idTicket);
}
