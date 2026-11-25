package com.apitracker.apitracker.controller;

import com.apitracker.apitracker.dto.UserResponse;
import com.apitracker.apitracker.exception.GlobalExceptionHandler;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getAllUsers_shouldReturn200WithCamelCaseJson() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse("user-jd", "John Doe", "JD", "#00875a"),
                new UserResponse("user-sc", "Sarah Connor", "SC", "#6554c0")
        ));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("user-jd")))
                .andExpect(jsonPath("$[0].name", is("John Doe")))
                .andExpect(jsonPath("$[0].initials", is("JD")))
                .andExpect(jsonPath("$[0].color", is("#00875a")));
    }

    @Test
    void getUserById_shouldReturn200WhenFound() throws Exception {
        when(userService.findById("user-ad"))
                .thenReturn(new UserResponse("user-ad", "Alex Dev", "AD", "#0052cc"));

        mockMvc.perform(get("/api/v1/users/user-ad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("user-ad")))
                .andExpect(jsonPath("$.name", is("Alex Dev")))
                .andExpect(jsonPath("$.initials", is("AD")))
                .andExpect(jsonPath("$.color", is("#0052cc")));
    }

    @Test
    void getUserById_shouldReturn404WhenNotFound() throws Exception {
        when(userService.findById("unknown"))
                .thenThrow(new ResourceNotFoundException("Utilisateur introuvable avec l'id : unknown"));

        mockMvc.perform(get("/api/v1/users/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Utilisateur introuvable avec l'id : unknown")))
                .andExpect(jsonPath("$.path", is("/api/v1/users/unknown")))
                .andExpect(jsonPath("$.validationErrors", hasSize(0)));
    }

    @Test
    void corsPreflight_shouldAllowViteOriginAndRejectUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
