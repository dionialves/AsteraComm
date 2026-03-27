package com.dionialves.AsteraComm.user;

import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.user.dto.UserResponseDTO;
import com.dionialves.AsteraComm.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        userResponseDTO = new UserResponseDTO(1L, "Test User", "user@test.com", UserRole.ADMIN, true,
                LocalDateTime.now(), LocalDateTime.now());

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getAll_shouldReturn200_withPagedResults() throws Exception {
        var page = new PageImpl<>(List.of(userResponseDTO), PageRequest.of(0, 10), 1);
        when(userService.findAll(any(), anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("user@test.com"));
    }

    @Test
    void getById_shouldReturn200_whenExists() throws Exception {
        when(userService.findById(1L)).thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user@test.com"));
    }

    @Test
    void getById_shouldReturn404_whenNotExists() throws Exception {
        when(userService.findById(99L)).thenThrow(new NotFoundException("User not found with ID: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn201_withValidBody() throws Exception {
        when(userService.create(any())).thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"Test User\",\"username\":\"user@test.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@test.com"));
    }

    @Test
    void create_shouldReturn400_withInvalidBody() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ab\",\"username\":\"u\",\"password\":\"123\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200_withValidBody() throws Exception {
        when(userService.update(eq(1L), any())).thenReturn(userResponseDTO);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Name\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updatePassword_shouldReturn200() throws Exception {
        when(userService.updatePassword(eq(1L), anyString())).thenReturn(userResponseDTO);

        mockMvc.perform(patch("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"newpassword123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/users/all ---

    @Test
    void getAll_shouldReturn200_withArray() throws Exception {
        var users = List.of(new UserResponseDTO(1L, "Admin", "admin@test.com", UserRole.ADMIN, true,
                LocalDateTime.now(), LocalDateTime.now()));
        when(userService.findAllSummary()).thenReturn(users);

        mockMvc.perform(get("/api/users/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Admin"))
                .andExpect(jsonPath("$[0].username").value("admin@test.com"));
    }

    @Test
    void getAll_shouldReturn200_withEmptyArray_whenNoUsers() throws Exception {
        when(userService.findAllSummary()).thenReturn(List.of());

        mockMvc.perform(get("/api/users/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
