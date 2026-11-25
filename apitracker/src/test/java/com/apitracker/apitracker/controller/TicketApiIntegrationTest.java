package com.apitracker.apitracker.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void seededDemoData_shouldMatchFrontendPiniaContract() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].id", hasItem("user-jd")))
                .andExpect(jsonPath("$[*].id", hasItem("user-sc")))
                .andExpect(jsonPath("$[*].id", hasItem("user-ad")))
                .andExpect(jsonPath("$[*].id", hasItem("user-pm")));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(3)))
                .andExpect(jsonPath("$[0].status", is("TODO")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$[2].id", is(1)))
                .andExpect(jsonPath("$[2].status", is("DONE")));

        mockMvc.perform(get("/api/v1/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.todo", is(1)))
                .andExpect(jsonPath("$.inProgress", is(1)))
                .andExpect(jsonPath("$.done", is(1)));
    }

    @Test
    void filteringAndKeySearch_shouldWorkAgainstRealH2Database() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("label", "UI/UX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));

        mockMvc.perform(get("/api/v1/tickets").param("search", "KAN-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));

        mockMvc.perform(get("/api/v1/tickets").param("status", "TODO").param("assigneeId", "user-sc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(3)));
    }

    @Test
    void fullTicketLifecycle_shouldPersistAndCleanupBidirectionalLinksInH2() throws Exception {
        String createPayload = """
                {
                  "title": "Intégrer API Spring Boot",
                  "description": "Connecter le store Pinia au backend",
                  "priority": "HIGH",
                  "dueDate": "2026-12-31",
                  "assigneeId": null,
                  "labels": ["Backend", "Feature"]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.labels", hasSize(2)))
                .andReturn();

        JsonNode createdJson = jsonMapper.readTree(createResult.getResponse().getContentAsString());
        long createdId = createdJson.get("id").asLong();

        mockMvc.perform(get("/api/v1/tickets").param("assigneeId", "UNASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) createdId)));

        mockMvc.perform(patch("/api/v1/tickets/{id}/status/toggle", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(post("/api/v1/tickets/{id}/labels/toggle", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Bug\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels", hasItem("Bug")));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Test E2E validé\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tickets/{id}/links", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTicketId\":3,\"type\":\"blocks\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("blocks")));

        mockMvc.perform(get("/api/v1/tickets/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[?(@.targetTicketId==" + createdId + ")].type", hasItem("is_blocked_by")));

        mockMvc.perform(delete("/api/v1/tickets/{id}", createdId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tickets/{id}", createdId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tickets/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[?(@.targetTicketId==" + createdId + ")]", hasSize(0)));
    }
}
