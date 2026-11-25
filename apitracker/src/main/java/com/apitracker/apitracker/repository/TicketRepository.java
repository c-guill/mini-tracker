package com.apitracker.apitracker.repository;

import com.apitracker.apitracker.domain.Ticket;
import com.apitracker.apitracker.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByStatus(TicketStatus status);
}
