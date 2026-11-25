package com.apitracker.apitracker.dto;

public record TicketCommentResponse(
        Long id,
        String author,
        String content,
        Long createdAt
) {
}
