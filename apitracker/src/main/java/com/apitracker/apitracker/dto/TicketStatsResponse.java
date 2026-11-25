package com.apitracker.apitracker.dto;

public record TicketStatsResponse(
        long total,
        long todo,
        long inProgress,
        long done
) {
}
