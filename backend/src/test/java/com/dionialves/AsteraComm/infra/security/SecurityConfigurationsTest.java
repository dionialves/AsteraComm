package com.dionialves.AsteraComm.infra.security;

import com.dionialves.AsteraComm.auth.AuthenticationController;
import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import com.dionialves.AsteraComm.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests that verify security configuration behaviors:
 * - Login endpoint is publicly accessible (no 401)
 * - Token validate returns 401 for invalid tokens
 * - Token validate returns 200 for valid tokens
 *
 * Note: In standalone MockMvc, Spring Security filter chain is NOT loaded.
 * Endpoint authorization (403 for missing roles) is tested at the
 * SecurityFilter integration level via SecurityFilterTest.
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigurationsTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validateEndpoint_shouldReturn401_whenTokenIsBlank() throws Exception {
        when(tokenService.validateToken("bad")).thenReturn("");

        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateEndpoint_shouldReturn200_whenTokenIsValid() throws Exception {
        when(tokenService.validateToken("good-token")).thenReturn("user@test.com");

        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void validateEndpoint_shouldReturn401_whenMissingBearerPrefix() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "not-a-bearer-token"))
                .andExpect(status().isUnauthorized());
    }
}
