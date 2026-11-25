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
import com.apitracker.apitracker.exception.DuplicateResourceException;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.repository.AppUserRepository;
import com.apitracker.apitracker.repository.IssueLinkRepository;
import com.apitracker.apitracker.repository.TicketCommentRepository;
import com.apitracker.apitracker.repository.TicketHistoryItemRepository;
import com.apitracker.apitracker.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private TicketHistoryItemRepository ticketHistoryItemRepository;
    @Mock
    private IssueLinkRepository issueLinkRepository;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository,
                appUserRepository,
                ticketCommentRepository,
                ticketHistoryItemRepository,
                issueLinkRepository,
                new TicketMapper()
        );
    }

    private Ticket buildTicket(Long id, String title, TicketStatus status, TicketPriority priority, String dueDate) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle(title);
        ticket.setDescription("Description " + id);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setCreatedAt(1700000000000L + id);
        ticket.setDueDate(dueDate);
        ticket.setAssigneeId("user-jd");
        ticket.setLabels(new ArrayList<>(List.of(TicketLabel.BACKEND)));
        return ticket;
    }

    @Test
    void findAll_shouldSortByIdDescByDefaultAndSupportAllSortFields() {
        Ticket t1 = buildTicket(1L, "beta", TicketStatus.DONE, TicketPriority.LOW, "2026-12-10");
        Ticket t2 = buildTicket(2L, "Alpha", TicketStatus.TODO, TicketPriority.HIGH, null);
        Ticket t3 = buildTicket(3L, "gamma", TicketStatus.IN_PROGRESS, TicketPriority.LOW, "2026-12-01");

        when(ticketRepository.findAll(ArgumentMatchers.<Specification<Ticket>>any())).thenReturn(List.of(t1, t2, t3));

        List<TicketResponse> defaultSort = ticketService.findAll(null, null, null, null, null, null);
        assertEquals(List.of(3L, 2L, 1L), defaultSort.stream().map(TicketResponse::id).toList());

        List<TicketResponse> titleAsc = ticketService.findAll(null, null, null, null, "title", "asc");
        assertEquals(List.of(2L, 1L, 3L), titleAsc.stream().map(TicketResponse::id).toList());

        List<TicketResponse> priorityDesc = ticketService.findAll(null, null, null, null, "priority", "desc");
        assertEquals(2L, priorityDesc.get(0).id());

        List<TicketResponse> statusAsc = ticketService.findAll(null, null, null, null, "status", "asc");
        assertEquals(List.of(2L, 3L, 1L), statusAsc.stream().map(TicketResponse::id).toList());

        List<TicketResponse> dueDateAsc = ticketService.findAll(null, null, null, null, "dueDate", "asc");
        assertEquals(List.of(3L, 1L, 2L), dueDateAsc.stream().map(TicketResponse::id).toList());

        List<TicketResponse> unknownSortAsc = ticketService.findAll(null, null, null, null, "unknownField", "asc");
        assertEquals(List.of(1L, 2L, 3L), unknownSortAsc.stream().map(TicketResponse::id).toList());
    }

    @Test
    void findById_shouldReturnTicketWhenExists() {
        Ticket ticket = buildTicket(1L, "Ticket 1", TicketStatus.TODO, TicketPriority.HIGH, "2026-12-01");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Ticket 1", response.title());
    }

    @Test
    void findById_shouldThrowWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.findById(99L));
    }

    @Test
    void getStats_shouldReturnCountsPerStatus() {
        when(ticketRepository.count()).thenReturn(6L);
        when(ticketRepository.countByStatus(TicketStatus.TODO)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.DONE)).thenReturn(1L);

        TicketStatsResponse stats = ticketService.getStats();

        assertEquals(new TicketStatsResponse(6L, 2L, 3L, 1L), stats);
    }

    @Test
    void create_shouldNormalizeFieldsDefaultStatusDeduplicateLabelsAndLogHistory() {
        when(appUserRepository.existsById("user-jd")).thenReturn(true);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CreateTicketRequest request = new CreateTicketRequest(
                "  Nouveau ticket  ",
                "   ",
                null,
                TicketPriority.HIGH,
                "",
                " user-jd ",
                Arrays.asList(TicketLabel.BACKEND, null, TicketLabel.BACKEND, TicketLabel.BUG)
        );

        TicketResponse response = ticketService.create(request);

        assertEquals(10L, response.id());
        assertEquals("Nouveau ticket", response.title());
        assertNull(response.description());
        assertEquals(TicketStatus.TODO, response.status());
        assertEquals(TicketPriority.HIGH, response.priority());
        assertNull(response.dueDate());
        assertEquals("user-jd", response.assigneeId());
        assertEquals(List.of(TicketLabel.BACKEND, TicketLabel.BUG), response.labels());
        assertEquals(1, response.history().size());
        assertEquals("John Doe", response.history().get(0).author());
        assertEquals("a créé la demande", response.history().get(0).action());
    }

    @Test
    void create_shouldThrowWhenAssigneeDoesNotExist() {
        when(appUserRepository.existsById("missing-user")).thenReturn(false);

        CreateTicketRequest request = new CreateTicketRequest(
                "Ticket",
                "Desc",
                TicketStatus.TODO,
                TicketPriority.LOW,
                null,
                "missing-user",
                null
        );

        assertThrows(ResourceNotFoundException.class, () -> ticketService.create(request));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_shouldNotValidateAssigneeWhenBlank() {
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        CreateTicketRequest request = new CreateTicketRequest(
                "Ticket sans responsable",
                "Desc",
                TicketStatus.IN_PROGRESS,
                TicketPriority.LOW,
                "2026-12-31",
                "   ",
                null
        );

        TicketResponse response = ticketService.create(request);

        assertNull(response.assigneeId());
        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertTrue(response.labels().isEmpty());
        verify(appUserRepository, never()).existsById(any());
    }

    @Test
    void update_shouldLogHistoryForEachChangedFieldOnly() {
        Ticket ticket = buildTicket(1L, "Ancien titre", TicketStatus.TODO, TicketPriority.LOW, "2026-12-01");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(appUserRepository.existsById("user-sc")).thenReturn(true);
        when(appUserRepository.findById("user-sc"))
                .thenReturn(Optional.of(new AppUser("user-sc", "Sarah Connor", "SC", "#6554c0")));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest(
                "Nouveau titre",
                "Nouvelle description",
                TicketStatus.DONE,
                TicketPriority.HIGH,
                "",
                "user-sc",
                List.of(TicketLabel.FRONTEND, TicketLabel.FRONTEND)
        );

        TicketResponse response = ticketService.update(1L, request);

        assertEquals("Nouveau titre", response.title());
        assertEquals("Nouvelle description", response.description());
        assertEquals(TicketStatus.DONE, response.status());
        assertEquals(TicketPriority.HIGH, response.priority());
        assertNull(response.dueDate());
        assertEquals("user-sc", response.assigneeId());
        assertEquals(List.of(TicketLabel.FRONTEND), response.labels());

        List<String> actions = response.history().stream().map(TicketHistoryItemResponse::action).toList();
        assertEquals(6, actions.size());
        assertTrue(actions.contains("a modifié le résumé"));
        assertTrue(actions.contains("a mis à jour la description"));
        assertTrue(actions.contains("a changé l'état"));
        assertTrue(actions.contains("a changé la priorité"));
        assertTrue(actions.contains("a mis à jour la date d'échéance"));
        assertTrue(actions.contains("a assigné la demande à"));

        TicketHistoryItemResponse dueDateLog = response.history().stream()
                .filter(h -> h.action().equals("a mis à jour la date d'échéance"))
                .findFirst()
                .orElseThrow();
        assertEquals("Retirée", dueDateLog.details());
    }

    @Test
    void update_shouldNotLogHistoryWhenFieldsAreUnchanged() {
        Ticket ticket = buildTicket(1L, "Titre", TicketStatus.TODO, TicketPriority.LOW, "2026-12-01");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(appUserRepository.existsById("user-jd")).thenReturn(true);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest(
                "Titre",
                "Description 1",
                TicketStatus.TODO,
                TicketPriority.LOW,
                "2026-12-01",
                "user-jd",
                List.of(TicketLabel.DEVOPS)
        );

        TicketResponse response = ticketService.update(1L, request);

        assertTrue(response.history().isEmpty());
        assertEquals(List.of(TicketLabel.DEVOPS), response.labels());
    }

    @Test
    void patch_shouldUpdateOnlyProvidedFieldsAndRejectBlankTitle() {
        Ticket ticket = buildTicket(1L, "Titre initial", TicketStatus.TODO, TicketPriority.LOW, "2026-12-01");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        PatchTicketRequest blankTitleRequest = new PatchTicketRequest("   ", null, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> ticketService.patch(1L, blankTitleRequest));

        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        PatchTicketRequest validPatch = new PatchTicketRequest(
                "Titre patché",
                null,
                TicketStatus.IN_PROGRESS,
                null,
                null,
                "",
                null
        );

        TicketResponse response = ticketService.patch(1L, validPatch);

        assertEquals("Titre patché", response.title());
        assertEquals("Description 1", response.description());
        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(TicketPriority.LOW, response.priority());
        assertEquals("2026-12-01", response.dueDate());
        assertNull(response.assigneeId());
        assertEquals(3, response.history().size());
    }

    @Test
    void updateStatus_andToggleStatus_shouldCycleStatusesAndLogHistory() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse sameStatus = ticketService.updateStatus(1L, new UpdateTicketStatusRequest(TicketStatus.TODO));
        assertTrue(sameStatus.history().isEmpty());

        TicketResponse step1 = ticketService.toggleStatus(1L);
        assertEquals(TicketStatus.IN_PROGRESS, step1.status());
        assertEquals("À FAIRE → EN COURS", step1.history().get(0).details());

        TicketResponse step2 = ticketService.toggleStatus(1L);
        assertEquals(TicketStatus.DONE, step2.status());
        assertEquals("EN COURS → TERMINÉ", step2.history().get(0).details());

        TicketResponse step3 = ticketService.toggleStatus(1L);
        assertEquals(TicketStatus.TODO, step3.status());
        assertEquals("TERMINÉ → À FAIRE", step3.history().get(0).details());
    }

    @Test
    void updatePriority_shouldLogOnlyWhenChanged() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse unchanged = ticketService.updatePriority(1L, new UpdateTicketPriorityRequest(TicketPriority.LOW));
        assertTrue(unchanged.history().isEmpty());

        TicketResponse changed = ticketService.updatePriority(1L, new UpdateTicketPriorityRequest(TicketPriority.HIGH));
        assertEquals(TicketPriority.HIGH, changed.priority());
        assertEquals("a changé la priorité", changed.history().get(0).action());
        assertEquals("Basse → Haute", changed.history().get(0).details());
    }

    @Test
    void updateAssignee_shouldValidateAndResolveNameOrUnassigned() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(appUserRepository.existsById("user-sc")).thenReturn(true);
        when(appUserRepository.findById("user-sc"))
                .thenReturn(Optional.of(new AppUser("user-sc", "Sarah Connor", "SC", "#6554c0")));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse assigned = ticketService.updateAssignee(1L, new UpdateTicketAssigneeRequest("user-sc"));
        assertEquals("user-sc", assigned.assigneeId());
        assertEquals("Sarah Connor", assigned.history().get(0).details());

        TicketResponse unassigned = ticketService.updateAssignee(1L, new UpdateTicketAssigneeRequest("  "));
        assertNull(unassigned.assigneeId());
        assertEquals("Non assigné", unassigned.history().get(0).details());
    }

    @Test
    void updateDueDate_shouldSetOrClearDueDateWithHistory() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse setResp = ticketService.updateDueDate(1L, new UpdateTicketDueDateRequest("2026-12-25"));
        assertEquals("2026-12-25", setResp.dueDate());
        assertEquals("Échéance : 2026-12-25", setResp.history().get(0).details());

        TicketResponse clearedResp = ticketService.updateDueDate(1L, new UpdateTicketDueDateRequest(""));
        assertNull(clearedResp.dueDate());
        assertEquals("Retirée", clearedResp.history().get(0).details());
    }

    @Test
    void toggleLabel_shouldAddThenRemoveLabelAndLogHistory() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse added = ticketService.toggleLabel(1L, new ToggleTicketLabelRequest(TicketLabel.BUG));
        assertTrue(added.labels().contains(TicketLabel.BUG));
        assertEquals("a ajouté l'étiquette « Bug »", added.history().get(0).action());

        TicketResponse removed = ticketService.toggleLabel(1L, new ToggleTicketLabelRequest(TicketLabel.BUG));
        assertTrue(!removed.labels().contains(TicketLabel.BUG));
        assertEquals("a retiré l'étiquette « Bug »", removed.history().get(0).action());
    }

    @Test
    void delete_shouldDeleteIncomingLinksBeforeDeletingTicket() {
        Ticket ticket = buildTicket(5L, "À supprimer", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        ticketService.delete(5L);

        InOrder order = inOrder(issueLinkRepository, ticketRepository);
        order.verify(issueLinkRepository).deleteByTargetTicketId(5L);
        order.verify(ticketRepository).delete(ticket);
    }

    @Test
    void addComment_andDeleteComment_shouldManageCommentsAndHistory() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.saveAndFlush(any(TicketComment.class))).thenAnswer(inv -> {
            TicketComment c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketCommentResponse added = ticketService.addComment(
                1L,
                new CreateCommentRequest("  Un commentaire utile  ", "   ")
        );

        assertEquals(100L, added.id());
        assertEquals("John Doe", added.author());
        assertEquals("Un commentaire utile", added.content());
        assertEquals(1, ticket.getHistory().size());
        assertEquals("a ajouté un commentaire", ticket.getHistory().get(0).getAction());

        TicketComment storedComment = ticket.getComments().get(0);
        when(ticketCommentRepository.findByIdAndTicketId(100L, 1L)).thenReturn(Optional.of(storedComment));

        ticketService.deleteComment(1L, 100L);

        assertTrue(ticket.getComments().isEmpty());
        verify(ticketCommentRepository).delete(storedComment);
        assertEquals("a supprimé un commentaire", ticket.getHistory().get(0).getAction());
    }

    @Test
    void deleteComment_shouldThrowWhenCommentNotFoundForTicket() {
        Ticket ticket = buildTicket(1L, "Ticket", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findByIdAndTicketId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.deleteComment(1L, 999L));
    }

    @Test
    void addIssueLink_shouldCreateMirrorLinkAndLogHistoryOnBothTickets() {
        Ticket source = buildTicket(1L, "Source", TicketStatus.TODO, TicketPriority.HIGH, null);
        Ticket target = buildTicket(2L, "Target", TicketStatus.TODO, TicketPriority.LOW, null);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(source));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(target));
        when(issueLinkRepository.existsBySourceTicketIdAndTargetTicketId(1L, 2L)).thenReturn(false);
        when(issueLinkRepository.existsBySourceTicketIdAndTargetTicketId(2L, 1L)).thenReturn(false);
        when(issueLinkRepository.saveAndFlush(any(IssueLink.class))).thenAnswer(inv -> {
            IssueLink link = inv.getArgument(0);
            if (link.getTargetTicketId().equals(2L)) {
                link.setId(301L);
            } else {
                link.setId(302L);
            }
            return link;
        });

        IssueLinkResponse response = ticketService.addIssueLink(
                1L,
                new CreateIssueLinkRequest(2L, IssueLinkType.BLOCKS)
        );

        assertEquals(301L, response.id());
        assertEquals(2L, response.targetTicketId());
        assertEquals(IssueLinkType.BLOCKS, response.type());

        assertEquals(1, source.getLinks().size());
        assertEquals(1, target.getLinks().size());
        assertEquals(IssueLinkType.IS_BLOCKED_BY, target.getLinks().get(0).getType());
        assertEquals("a lié cette demande (bloque KAN-2)", source.getHistory().get(0).getAction());
        assertEquals("a été liée (est bloqué par KAN-1)", target.getHistory().get(0).getAction());
    }

    @Test
    void addIssueLink_shouldRejectSelfLinkAndDuplicateLink() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ticketService.addIssueLink(1L, new CreateIssueLinkRequest(1L, IssueLinkType.RELATES_TO))
        );

        Ticket source = buildTicket(1L, "Source", TicketStatus.TODO, TicketPriority.HIGH, null);
        Ticket target = buildTicket(2L, "Target", TicketStatus.TODO, TicketPriority.LOW, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(source));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(target));
        when(issueLinkRepository.existsBySourceTicketIdAndTargetTicketId(1L, 2L)).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> ticketService.addIssueLink(1L, new CreateIssueLinkRequest(2L, IssueLinkType.BLOCKS))
        );
    }

    @Test
    void removeIssueLink_shouldDeleteBothDirectionsAndLogHistory() {
        Ticket source = buildTicket(1L, "Source", TicketStatus.TODO, TicketPriority.HIGH, null);
        Ticket target = buildTicket(2L, "Target", TicketStatus.TODO, TicketPriority.LOW, null);
        IssueLink sourceLink = new IssueLink(2L, IssueLinkType.BLOCKS);
        sourceLink.setId(301L);
        source.addLink(sourceLink);
        IssueLink mirrorLink = new IssueLink(1L, IssueLinkType.IS_BLOCKED_BY);
        mirrorLink.setId(302L);
        target.addLink(mirrorLink);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(source));
        when(issueLinkRepository.findBySourceTicketIdAndTargetTicketId(1L, 2L)).thenReturn(Optional.of(sourceLink));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(target));
        when(issueLinkRepository.findBySourceTicketIdAndTargetTicketId(2L, 1L)).thenReturn(Optional.of(mirrorLink));

        ticketService.removeIssueLink(1L, 2L);

        assertTrue(source.getLinks().isEmpty());
        assertTrue(target.getLinks().isEmpty());
        verify(issueLinkRepository).delete(sourceLink);
        verify(issueLinkRepository).delete(mirrorLink);
        assertEquals("a supprimé le lien avec KAN-2", source.getHistory().get(0).getAction());
        assertEquals("a supprimé le lien avec KAN-1", target.getHistory().get(0).getAction());
    }

    @Test
    void removeIssueLink_shouldThrowWhenLinkMissing() {
        Ticket source = buildTicket(1L, "Source", TicketStatus.TODO, TicketPriority.HIGH, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(source));
        when(issueLinkRepository.findBySourceTicketIdAndTargetTicketId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.removeIssueLink(1L, 99L));
    }

    @Test
    void getHistory_shouldReturnHistoryWhenTicketExistsAndThrowWhenMissing() {
        when(ticketRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> ticketService.getHistory(99L));

        when(ticketRepository.existsById(1L)).thenReturn(true);
        TicketHistoryItem item = new TicketHistoryItem("John Doe", "a créé la demande", null, 1700000000000L);
        item.setId(1L);
        when(ticketHistoryItemRepository.findByTicketIdOrderByTimestampDescIdDesc(1L)).thenReturn(List.of(item));

        List<TicketHistoryItemResponse> history = ticketService.getHistory(1L);

        assertEquals(1, history.size());
        assertEquals("a créé la demande", history.get(0).action());
    }
}
