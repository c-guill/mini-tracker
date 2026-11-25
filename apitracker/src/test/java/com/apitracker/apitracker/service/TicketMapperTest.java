package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.domain.IssueLink;
import com.apitracker.apitracker.domain.IssueLinkType;
import com.apitracker.apitracker.domain.Ticket;
import com.apitracker.apitracker.domain.TicketComment;
import com.apitracker.apitracker.domain.TicketHistoryItem;
import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketPriority;
import com.apitracker.apitracker.domain.TicketStatus;
import com.apitracker.apitracker.dto.IssueLinkResponse;
import com.apitracker.apitracker.dto.TicketCommentResponse;
import com.apitracker.apitracker.dto.TicketHistoryItemResponse;
import com.apitracker.apitracker.dto.TicketResponse;
import com.apitracker.apitracker.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketMapperTest {

    private final TicketMapper mapper = new TicketMapper();

    @Test
    void toUserResponse_shouldMapAllFields() {
        AppUser user = new AppUser("user-pm", "Chef de projet", "MT", "#ffab00");

        UserResponse response = mapper.toUserResponse(user);

        assertEquals("user-pm", response.id());
        assertEquals("Chef de projet", response.name());
        assertEquals("MT", response.initials());
        assertEquals("#ffab00", response.color());
    }

    @Test
    void toTicketResponse_shouldMapTicketAndNestedCollections() {
        Ticket ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTitle("Corriger le bug CSS");
        ticket.setDescription("Alignement Kanban");
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setCreatedAt(1700000000000L);
        ticket.setDueDate("2026-12-01");
        ticket.setAssigneeId("user-jd");
        ticket.setLabels(List.of(TicketLabel.FRONTEND, TicketLabel.UI_UX));

        TicketComment comment = new TicketComment("John Doe", "Premier retour", 1700000001000L);
        comment.setId(10L);
        ticket.addComment(comment);

        TicketHistoryItem historyItem = new TicketHistoryItem("John Doe", "a créé la demande", null, 1700000000000L);
        historyItem.setId(20L);
        ticket.addHistoryItem(historyItem);

        IssueLink link = new IssueLink(2L, IssueLinkType.BLOCKS);
        link.setId(30L);
        ticket.addLink(link);

        TicketResponse response = mapper.toTicketResponse(ticket);

        assertEquals(42L, response.id());
        assertEquals("Corriger le bug CSS", response.title());
        assertEquals("Alignement Kanban", response.description());
        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(TicketPriority.HIGH, response.priority());
        assertEquals(1700000000000L, response.createdAt());
        assertEquals("2026-12-01", response.dueDate());
        assertEquals("user-jd", response.assigneeId());
        assertEquals(List.of(TicketLabel.FRONTEND, TicketLabel.UI_UX), response.labels());
        assertThrows(UnsupportedOperationException.class, () -> response.labels().add(TicketLabel.BUG));

        assertEquals(1, response.comments().size());
        TicketCommentResponse commentResponse = response.comments().get(0);
        assertEquals(10L, commentResponse.id());
        assertEquals("John Doe", commentResponse.author());
        assertEquals("Premier retour", commentResponse.content());
        assertEquals(1700000001000L, commentResponse.createdAt());

        assertEquals(1, response.history().size());
        TicketHistoryItemResponse historyResponse = response.history().get(0);
        assertEquals(20L, historyResponse.id());
        assertEquals("a créé la demande", historyResponse.action());

        assertEquals(1, response.links().size());
        IssueLinkResponse linkResponse = response.links().get(0);
        assertEquals(30L, linkResponse.id());
        assertEquals(2L, linkResponse.targetTicketId());
        assertEquals(IssueLinkType.BLOCKS, linkResponse.type());
    }
}
