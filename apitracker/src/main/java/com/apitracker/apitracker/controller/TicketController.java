package com.apitracker.apitracker.controller;

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
import com.apitracker.apitracker.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) TicketLabel label,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
        return ResponseEntity.ok(ticketService.findAll(status, assigneeId, label, search, sortBy, sortOrder));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsResponse> getStats() {
        return ResponseEntity.ok(ticketService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return ResponseEntity.ok(ticketService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> patchTicket(
            @PathVariable Long id,
            @Valid @RequestBody PatchTicketRequest request
    ) {
        return ResponseEntity.ok(ticketService.patch(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/{id}/status/toggle", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<TicketResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.toggleStatus(id));
    }

    @PostMapping("/reset")
    public ResponseEntity<List<TicketResponse>> reset() {
        return ResponseEntity.ok(ticketService.resetToDemoData());
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<TicketResponse> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketPriorityRequest request
    ) {
        return ResponseEntity.ok(ticketService.updatePriority(id, request));
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<TicketResponse> updateAssignee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketAssigneeRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateAssignee(id, request));
    }

    @PatchMapping("/{id}/due-date")
    public ResponseEntity<TicketResponse> updateDueDate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketDueDateRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateDueDate(id, request));
    }

    @PostMapping("/{id}/labels/toggle")
    public ResponseEntity<TicketResponse> toggleLabel(
            @PathVariable Long id,
            @Valid @RequestBody ToggleTicketLabelRequest request
    ) {
        return ResponseEntity.ok(ticketService.toggleLabel(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        TicketCommentResponse created = ticketService.addComment(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{commentId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId
    ) {
        ticketService.deleteComment(id, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<IssueLinkResponse> addIssueLink(
            @PathVariable Long id,
            @Valid @RequestBody CreateIssueLinkRequest request
    ) {
        IssueLinkResponse created = ticketService.addIssueLink(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{targetTicketId}")
                .buildAndExpand(created.targetTicketId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}/links/{targetTicketId}")
    public ResponseEntity<Void> removeIssueLink(
            @PathVariable Long id,
            @PathVariable Long targetTicketId
    ) {
        ticketService.removeIssueLink(id, targetTicketId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketHistoryItemResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getHistory(id));
    }
}
