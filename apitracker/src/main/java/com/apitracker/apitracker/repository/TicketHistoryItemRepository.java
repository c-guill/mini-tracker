package com.apitracker.apitracker.repository;

import com.apitracker.apitracker.domain.TicketHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryItemRepository extends JpaRepository<TicketHistoryItem, Long> {

    List<TicketHistoryItem> findByTicketIdOrderByTimestampDescIdDesc(Long ticketId);
}
