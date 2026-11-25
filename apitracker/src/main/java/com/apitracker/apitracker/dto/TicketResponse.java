package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketPriority;
import com.apitracker.apitracker.domain.TicketStatus;

import java.util.List;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long createdAt,
        String dueDate,
        String assigneeId,
        List<TicketLabel> labels,
        List<TicketCommentResponse> comments,
        List<TicketHistoryItemResponse> history,
        List<IssueLinkResponse> links
) {
}
