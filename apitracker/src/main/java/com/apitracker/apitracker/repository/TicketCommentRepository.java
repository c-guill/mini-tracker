package com.apitracker.apitracker.repository;

import com.apitracker.apitracker.domain.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    Optional<TicketComment> findByIdAndTicketId(Long id, Long ticketId);
}
