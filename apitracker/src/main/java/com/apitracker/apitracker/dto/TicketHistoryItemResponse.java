package com.apitracker.apitracker.dto;

public record TicketHistoryItemResponse(
        Long id,
        String author,
        String action,
        String details,
        Long timestamp
) {
}
