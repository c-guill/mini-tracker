package com.apitracker.apitracker.repository;

import com.apitracker.apitracker.domain.IssueLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueLinkRepository extends JpaRepository<IssueLink, Long> {

    boolean existsBySourceTicketIdAndTargetTicketId(Long sourceTicketId, Long targetTicketId);

    Optional<IssueLink> findBySourceTicketIdAndTargetTicketId(Long sourceTicketId, Long targetTicketId);

    void deleteByTargetTicketId(Long targetTicketId);
}
