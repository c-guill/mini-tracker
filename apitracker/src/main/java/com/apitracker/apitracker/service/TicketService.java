package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketStatus;
import com.apitracker.apitracker.dto.CreateCommentRequest;
import com.apitracker.apitracker.dto.CreateIssueLinkRequest;
import com.apitracker.apitracker.dto.CreateTicketRequest;
import com.apitracker.apitracker.dto.IssueLinkResponse;
import com.apitracker.apitracker.dto.PatchTicketRequest;
import com.apitracker.apitracker.dto.TicketCommentResponse;
import com.apitracker.apitracker.dto.TicketHistoryItemResponse;
import com.apitracker.apitracker.dto.TicketResponse;
import com.apitracker.apitracker.dto.TicketStatsResponse;
import com.apitracker.apitracker.dto.ToggleTicketLabelRequest;
import com.apitracker.apitracker.dto.UpdateTicketAssigneeRequest;
import com.apitracker.apitracker.dto.UpdateTicketDueDateRequest;
import com.apitracker.apitracker.dto.UpdateTicketPriorityRequest;
import com.apitracker.apitracker.dto.UpdateTicketRequest;
import com.apitracker.apitracker.dto.UpdateTicketStatusRequest;

import java.util.List;

public interface TicketService {

    List<TicketResponse> findAll(
            TicketStatus status,
            String assigneeId,
            TicketLabel label,
            String search,
            String sortBy,
            String sortOrder
    );

    TicketResponse findById(Long id);

    TicketStatsResponse getStats();

    TicketResponse create(CreateTicketRequest request);

    TicketResponse update(Long id, UpdateTicketRequest request);

    TicketResponse patch(Long id, PatchTicketRequest request);

    TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request);

    TicketResponse toggleStatus(Long id);

    TicketResponse updatePriority(Long id, UpdateTicketPriorityRequest request);

    TicketResponse updateAssignee(Long id, UpdateTicketAssigneeRequest request);

    TicketResponse updateDueDate(Long id, UpdateTicketDueDateRequest request);

    TicketResponse toggleLabel(Long id, ToggleTicketLabelRequest request);

    void delete(Long id);

    TicketCommentResponse addComment(Long ticketId, CreateCommentRequest request);

    void deleteComment(Long ticketId, Long commentId);

    IssueLinkResponse addIssueLink(Long ticketId, CreateIssueLinkRequest request);

    void removeIssueLink(Long ticketId, Long targetTicketId);

    List<TicketHistoryItemResponse> getHistory(Long ticketId);

    List<TicketResponse> resetToDemoData();
}
