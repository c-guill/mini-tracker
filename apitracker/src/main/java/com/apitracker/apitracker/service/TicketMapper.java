package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.domain.IssueLink;
import com.apitracker.apitracker.domain.Ticket;
import com.apitracker.apitracker.domain.TicketComment;
import com.apitracker.apitracker.domain.TicketHistoryItem;
import com.apitracker.apitracker.dto.IssueLinkResponse;
import com.apitracker.apitracker.dto.TicketCommentResponse;
import com.apitracker.apitracker.dto.TicketHistoryItemResponse;
import com.apitracker.apitracker.dto.TicketResponse;
import com.apitracker.apitracker.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketMapper {

    public UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getInitials(),
                user.getColor()
        );
    }

    public TicketCommentResponse toCommentResponse(TicketComment comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public TicketHistoryItemResponse toHistoryResponse(TicketHistoryItem item) {
        return new TicketHistoryItemResponse(
                item.getId(),
                item.getAuthor(),
                item.getAction(),
                item.getDetails(),
                item.getTimestamp()
        );
    }

    public IssueLinkResponse toLinkResponse(IssueLink link) {
        return new IssueLinkResponse(
                link.getId(),
                link.getTargetTicketId(),
                link.getType()
        );
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        List<TicketCommentResponse> comments = ticket.getComments().stream()
                .map(this::toCommentResponse)
                .toList();
        List<TicketHistoryItemResponse> history = ticket.getHistory().stream()
                .map(this::toHistoryResponse)
                .toList();
        List<IssueLinkResponse> links = ticket.getLinks().stream()
                .map(this::toLinkResponse)
                .toList();

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCreatedAt(),
                ticket.getDueDate(),
                ticket.getAssigneeId(),
                List.copyOf(ticket.getLabels()),
                comments,
                history,
                links
        );
    }
}
