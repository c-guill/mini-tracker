package com.apitracker.apitracker.controller;

import com.apitracker.apitracker.domain.IssueLinkType;
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
import com.apitracker.apitracker.exception.GlobalExceptionHandler;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TicketService ticketService;

    private TicketResponse sampleTicket(Long id) {
        return new TicketResponse(
                id,
                "Implémenter API",
                "Créer les contrôleurs REST",
                TicketStatus.IN_PROGRESS,
                TicketPriority.HIGH,
                1700000000000L,
                "2026-12-31",
                "user-jd",
                List.of(TicketLabel.BACKEND, TicketLabel.UI_UX),
                List.of(new TicketCommentResponse(10L, "John Doe", "En cours", 1700000001000L)),
                List.of(new TicketHistoryItemResponse(20L, "John Doe", "a créé la demande", null, 1700000000000L)),
                List.of(new IssueLinkResponse(30L, 2L, IssueLinkType.BLOCKS))
        );
    }

    @Test
    void getAllTickets_shouldDelegateQueryParamsAndReturnCamelCaseList() throws Exception {
        when(ticketService.findAll(
                TicketStatus.IN_PROGRESS,
                "user-jd",
                TicketLabel.UI_UX,
                "API",
                "priority",
                "asc"
        )).thenReturn(List.of(sampleTicket(1L)));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("status", "IN_PROGRESS")
                        .param("assigneeId", "user-jd")
                        .param("label", "UI/UX")
                        .param("search", "API")
                        .param("sortBy", "priority")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].createdAt", is(1700000000000L)))
                .andExpect(jsonPath("$[0].dueDate", is("2026-12-31")))
                .andExpect(jsonPath("$[0].assigneeId", is("user-jd")))
                .andExpect(jsonPath("$[0].labels[0]", is("Backend")))
                .andExpect(jsonPath("$[0].labels[1]", is("UI/UX")))
                .andExpect(jsonPath("$[0].links[0].targetTicketId", is(2)))
                .andExpect(jsonPath("$[0].links[0].type", is("blocks")));
    }

    @Test
    void getAllTickets_shouldReturn400WhenEnumParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("label", "INVALID_LABEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/v1/tickets")));
    }

    @Test
    void getStats_shouldReturn200WithCamelCaseStats() throws Exception {
        when(ticketService.getStats()).thenReturn(new TicketStatsResponse(5L, 2L, 2L, 1L));

        mockMvc.perform(get("/api/v1/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.todo", is(2)))
                .andExpect(jsonPath("$.inProgress", is(2)))
                .andExpect(jsonPath("$.done", is(1)));
    }

    @Test
    void getTicketById_shouldReturn200Or404Or400OnInvalidId() throws Exception {
        when(ticketService.findById(1L)).thenReturn(sampleTicket(1L));
        when(ticketService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Ticket introuvable avec l'id : 99"));

        mockMvc.perform(get("/api/v1/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Implémenter API")));

        mockMvc.perform(get("/api/v1/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Ticket introuvable avec l'id : 99")));

        mockMvc.perform(get("/api/v1/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void createTicket_shouldReturn201WithLocationHeader() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                "Nouveau ticket",
                "Description",
                TicketStatus.TODO,
                TicketPriority.HIGH,
                "2026-12-15",
                "user-jd",
                List.of(TicketLabel.FEATURE)
        );
        when(ticketService.create(any(CreateTicketRequest.class))).thenReturn(sampleTicket(10L));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tickets/10")))
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    void createTicket_shouldReturn400WithValidationErrorsWhenInvalid() throws Exception {
        String invalidJson = """
                {
                  "title": "   ",
                  "priority": null,
                  "dueDate": "31/12/2026"
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(3)))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("priority")))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("dueDate")));
    }

    @Test
    void createTicket_shouldReturn400WhenJsonBodyIsMalformedOrEnumInvalid() throws Exception {
        String invalidEnumJson = """
                {
                  "title": "Valide",
                  "priority": "CRITICAL"
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnumJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void updateTicket_shouldReturn200AndRejectInvalidPayload() throws Exception {
        UpdateTicketRequest valid = new UpdateTicketRequest(
                "Titre MAJ",
                "Desc MAJ",
                TicketStatus.DONE,
                TicketPriority.LOW,
                "2026-12-20",
                "user-sc",
                List.of(TicketLabel.DEVOPS)
        );
        when(ticketService.update(eq(1L), any(UpdateTicketRequest.class))).thenReturn(sampleTicket(1L));

        mockMvc.perform(put("/api/v1/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(valid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        String missingStatus = """
                {
                  "title": "Titre",
                  "priority": "LOW"
                }
                """;
        mockMvc.perform(put("/api/v1/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingStatus))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("status")));
    }

    @Test
    void patchTicket_shouldReturn200AndMapIllegalArgumentTo400() throws Exception {
        PatchTicketRequest valid = new PatchTicketRequest("Titre patché", null, null, null, null, null, null);
        when(ticketService.patch(eq(1L), any(PatchTicketRequest.class))).thenReturn(sampleTicket(1L));

        mockMvc.perform(patch("/api/v1/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(valid)))
                .andExpect(status().isOk());

        when(ticketService.patch(eq(1L), any(PatchTicketRequest.class)))
                .thenThrow(new IllegalArgumentException("Le titre ne peut pas être vide"));

        mockMvc.perform(patch("/api/v1/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Le titre ne peut pas être vide")));
    }

    @Test
    void subResourcePatchEndpoints_shouldValidateAndReturn200() throws Exception {
        TicketResponse response = sampleTicket(1L);
        when(ticketService.updateStatus(eq(1L), any(UpdateTicketStatusRequest.class))).thenReturn(response);
        when(ticketService.toggleStatus(1L)).thenReturn(response);
        when(ticketService.updatePriority(eq(1L), any(UpdateTicketPriorityRequest.class))).thenReturn(response);
        when(ticketService.updateAssignee(eq(1L), any(UpdateTicketAssigneeRequest.class))).thenReturn(response);
        when(ticketService.updateDueDate(eq(1L), any(UpdateTicketDueDateRequest.class))).thenReturn(response);
        when(ticketService.toggleLabel(eq(1L), any(ToggleTicketLabelRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/tickets/1/status/toggle"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tickets/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tickets/1/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":null}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tickets/1/due-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"2026-12-31\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tickets/1/due-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"invalid-date\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/tickets/1/labels/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"UI/UX\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tickets/1/labels/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTicket_shouldReturn204Or404() throws Exception {
        doNothing().when(ticketService).delete(1L);
        doThrow(new ResourceNotFoundException("Ticket introuvable avec l'id : 99")).when(ticketService).delete(99L);

        mockMvc.perform(delete("/api/v1/tickets/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/tickets/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentsEndpoints_shouldReturn201And204And400OnBlankContent() throws Exception {
        TicketCommentResponse comment = new TicketCommentResponse(10L, "John Doe", "Top", 1700000001000L);
        when(ticketService.addComment(eq(1L), any(CreateCommentRequest.class))).thenReturn(comment);
        doNothing().when(ticketService).deleteComment(1L, 10L);

        mockMvc.perform(post("/api/v1/tickets/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CreateCommentRequest("Top", "John Doe"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tickets/1/comments/10")))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.content", is("Top")));

        mockMvc.perform(post("/api/v1/tickets/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("content")));

        mockMvc.perform(delete("/api/v1/tickets/1/comments/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void issueLinksEndpoints_shouldHandle201And409And400And204() throws Exception {
        IssueLinkResponse link = new IssueLinkResponse(30L, 2L, IssueLinkType.IS_BLOCKED_BY);
        when(ticketService.addIssueLink(eq(1L), any(CreateIssueLinkRequest.class))).thenReturn(link);
        doNothing().when(ticketService).removeIssueLink(1L, 2L);

        mockMvc.perform(post("/api/v1/tickets/1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTicketId\":2,\"type\":\"is_blocked_by\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tickets/1/links/2")))
                .andExpect(jsonPath("$.targetTicketId", is(2)))
                .andExpect(jsonPath("$.type", is("is_blocked_by")));

        when(ticketService.addIssueLink(eq(1L), any(CreateIssueLinkRequest.class)))
                .thenThrow(new DuplicateResourceException("Un lien existe déjà"));

        mockMvc.perform(post("/api/v1/tickets/1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTicketId\":2,\"type\":\"blocks\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));

        mockMvc.perform(delete("/api/v1/tickets/1/links/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getHistory_shouldReturn200AndUnexpectedErrorShouldReturn500WithoutLeakingDetails() throws Exception {
        when(ticketService.getHistory(1L)).thenReturn(List.of(
                new TicketHistoryItemResponse(1L, "John Doe", "a créé la demande", null, 1700000000000L)
        ));

        mockMvc.perform(get("/api/v1/tickets/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action", is("a créé la demande")))
                .andExpect(jsonPath("$[0].details", nullValue()));

        when(ticketService.getHistory(999L))
                .thenThrow(new RuntimeException("Sensitive DB stack info: jdbc:h2:mem:secret"));

        mockMvc.perform(get("/api/v1/tickets/999/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("Une erreur interne est survenue")));
    }
}
