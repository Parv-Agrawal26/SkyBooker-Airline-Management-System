package com.skybooker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.auth.controller.AuthController;
import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.auth.security.JwtFilter.class
    )
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // JwtUtil and PasswordEncoder needed by SecurityConfig bean
    @MockBean
    private com.skybooker.auth.security.JwtUtil jwtUtil;

    @Test
    void register_ShouldReturn200_WhenValidRequest() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Rahul Sharma");
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");
        req.setRole("PASSENGER");

        AuthResponse response = AuthResponse.successMessage("Registration successful! Role assigned: PASSENGER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful! Role assigned: PASSENGER"));
    }

    @Test
    void login_ShouldReturn200_WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");

        AuthResponse response = AuthResponse.token("fake.jwt.token");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"));
    }

    @Test
    @WithMockUser
    void register_WhenServiceThrows_ShouldReturn500() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test");
        req.setEmail("test@gmail.com");
        req.setPassword("Test@1234");
        req.setRole("PASSENGER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already registered: test@gmail.com"));

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }
}
