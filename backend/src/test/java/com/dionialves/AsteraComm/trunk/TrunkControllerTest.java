package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.trunk.dto.TrunkCreateDTO;
import com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrunkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrunkService trunkService;

    @InjectMocks
    private TrunkController trunkController;

    private Trunk testTrunk;

    @BeforeEach
    void setUp() {
        testTrunk = new Trunk();
        testTrunk.setId(1L);
        testTrunk.setName("provedor1");
        testTrunk.setHost("sip.provedor1.com.br");
        testTrunk.setUsername("user123");
        testTrunk.setPassword("senha123");

        mockMvc = MockMvcBuilders.standaloneSetup(trunkController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getAll_shouldReturn200_paginated() throws Exception {
        var page = new PageImpl<TrunkProjection>(List.of(), PageRequest.of(0, 10), 0);
        when(trunkService.getAll(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/trunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_withSearch_shouldPassParam() throws Exception {
        var page = new PageImpl<TrunkProjection>(List.of(), PageRequest.of(0, 10), 0);
        when(trunkService.getAll(eq("provedor1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/trunks?search=provedor1"))
                .andExpect(status().isOk());

        verify(trunkService).getAll(eq("provedor1"), any());
    }

    @Test
    void getById_shouldReturn200_whenExists() throws Exception {
        when(trunkService.findByName("provedor1")).thenReturn(Optional.of(testTrunk));

        mockMvc.perform(get("/api/trunks/provedor1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("provedor1"))
                .andExpect(jsonPath("$.host").value("sip.provedor1.com.br"));
    }

    @Test
    void getById_shouldReturn404_whenNotExists() throws Exception {
        when(trunkService.findByName("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trunks/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn201_withValidBody() throws Exception {
        when(trunkService.create(any(TrunkCreateDTO.class))).thenReturn(testTrunk);

        mockMvc.perform(post("/api/trunks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"provedor1\",\"host\":\"sip.provedor1.com.br\",\"username\":\"user123\",\"password\":\"senha123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_shouldReturn400_whenNameAlreadyExists() throws Exception {
        when(trunkService.create(any(TrunkCreateDTO.class)))
                .thenThrow(new BusinessException("Tronco já existe"));

        mockMvc.perform(post("/api/trunks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"provedor1\",\"host\":\"sip.provedor1.com.br\",\"username\":\"user123\",\"password\":\"senha123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200_withValidBody() throws Exception {
        when(trunkService.update(eq("provedor1"), any(TrunkCreateDTO.class))).thenReturn(testTrunk);

        mockMvc.perform(put("/api/trunks/provedor1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"provedor1\",\"host\":\"novo.host.com\",\"username\":\"user123\",\"password\":\"novasenha\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn404_whenNotExists() throws Exception {
        when(trunkService.update(eq("inexistente"), any(TrunkCreateDTO.class)))
                .thenThrow(new NotFoundException("Tronco não encontrado"));

        mockMvc.perform(put("/api/trunks/inexistente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"inexistente\",\"host\":\"host.com\",\"username\":\"user\",\"password\":\"pass\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(trunkService).delete("provedor1");

        mockMvc.perform(delete("/api/trunks/provedor1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotExists() throws Exception {
        doThrow(new NotFoundException("Tronco não encontrado"))
                .when(trunkService).delete("inexistente");

        mockMvc.perform(delete("/api/trunks/inexistente"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/trunks/all ---

    @Test
    void getAll_shouldReturn200_withArray() throws Exception {
        var summaries = List.of(new TrunkSummaryDTO("provedor1"));
        when(trunkService.findAllSummary()).thenReturn(summaries);

        mockMvc.perform(get("/api/trunks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("provedor1"));
    }

    @Test
    void getAll_shouldReturn200_withEmptyArray_whenNoTrunks() throws Exception {
        when(trunkService.findAllSummary()).thenReturn(List.of());

        mockMvc.perform(get("/api/trunks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
