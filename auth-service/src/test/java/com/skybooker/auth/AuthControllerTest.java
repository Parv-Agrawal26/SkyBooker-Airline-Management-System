package com.skybooker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.auth.controller.AuthController;
import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.auth.security.JwtFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.skybooker.auth.security.JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void register_ShouldReturn200_WhenValidRequest() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Rahul Sharma");
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");
        req.setRole("PASSENGER");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.successMessage("Registration successful! Role assigned: PASSENGER"));

        mockMvc.perform(post("/auth/register")
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

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.token("fake.jwt.token"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"));
    }

    @Test
    void register_WhenServiceThrows_ShouldReturnErrorStatus() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test");
        req.setEmail("test@gmail.com");
        req.setPassword("Test@1234");
        req.setRole("PASSENGER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already registered: test@gmail.com"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }
}
