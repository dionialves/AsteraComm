package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.did.dto.DIDCircuitDTO;
import com.dionialves.AsteraComm.did.dto.DIDCreateDTO;
import com.dionialves.AsteraComm.did.dto.DIDResponseDTO;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.ConflictException;
import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import com.dionialves.AsteraComm.exception.NotFoundException;
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
class DIDControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DIDService didService;

    @InjectMocks
    private DIDController didController;

    private DID testDID;
    private DIDResponseDTO testDIDFreeDTO;
    private DIDResponseDTO testDIDInUseDTO;

    @BeforeEach
    void setUp() {
        testDID = new DID();
        testDID.setId(1L);
        testDID.setNumber("4933001234");
        testDID.setCircuit(null);

        testDIDFreeDTO  = new DIDResponseDTO(1L, "4933001234", "FREE",  null);
        testDIDInUseDTO = new DIDResponseDTO(1L, "4933001234", "IN_USE", new DIDCircuitDTO(1L, "100000"));

        mockMvc = MockMvcBuilders.standaloneSetup(didController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getAll_shouldReturn200_paginated() throws Exception {
        var page = new PageImpl<>(List.of(testDIDFreeDTO), PageRequest.of(0, 20), 1);
        when(didService.getAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/dids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_withInUseStatus_shouldPassFilterToService() throws Exception {
        var page = new PageImpl<>(List.of(testDIDInUseDTO), PageRequest.of(0, 20), 1);
        when(didService.getAll(any(), eq("IN_USE"), any())).thenReturn(page);

        mockMvc.perform(get("/api/dids").param("status", "IN_USE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("IN_USE"));

        verify(didService).getAll(any(), eq("IN_USE"), any());
    }

    @Test
    void getAll_withFreeStatus_shouldPassFilterToService() throws Exception {
        var page = new PageImpl<>(List.of(testDIDFreeDTO), PageRequest.of(0, 20), 1);
        when(didService.getAll(any(), eq("FREE"), any())).thenReturn(page);

        mockMvc.perform(get("/api/dids").param("status", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FREE"));

        verify(didService).getAll(any(), eq("FREE"), any());
    }

    @Test
    void getAll_shouldReturnCircuitInResponseWhenInUse() throws Exception {
        var page = new PageImpl<>(List.of(testDIDInUseDTO), PageRequest.of(0, 20), 1);
        when(didService.getAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/dids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].circuit.code").value("100000"));
    }

    @Test
    void getAll_shouldReturnNullCircuitWhenFree() throws Exception {
        var page = new PageImpl<>(List.of(testDIDFreeDTO), PageRequest.of(0, 20), 1);
        when(didService.getAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/dids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].circuit").doesNotExist());
    }

    @Test
    void getById_shouldReturn200_whenExists() throws Exception {
        when(didService.findById(1L)).thenReturn(Optional.of(testDID));

        mockMvc.perform(get("/api/dids/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("4933001234"));
    }

    @Test
    void getById_shouldReturn404_whenNotExists() throws Exception {
        when(didService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dids/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFree_shouldReturn200_withList() throws Exception {
        when(didService.getFree()).thenReturn(List.of(testDID));

        mockMvc.perform(get("/api/dids/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].number").value("4933001234"));
    }

    @Test
    void create_shouldReturn201_withValidBody() throws Exception {
        when(didService.create(any(DIDCreateDTO.class))).thenReturn(testDID);

        mockMvc.perform(post("/api/dids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"4933001234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("4933001234"));
    }

    @Test
    void create_shouldReturn400_whenNumberAlreadyExists() throws Exception {
        when(didService.create(any(DIDCreateDTO.class)))
                .thenThrow(new BusinessException("DID já cadastrado"));

        mockMvc.perform(post("/api/dids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"4933001234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenNumberContainsNonDigits() throws Exception {
        when(didService.create(any(DIDCreateDTO.class)))
                .thenThrow(new BusinessException("apenas dígitos"));

        mockMvc.perform(post("/api/dids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"(49) 3300-1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkToCircuit_shouldReturn200() throws Exception {
        Circuit circuit = new Circuit();
        circuit.setId(1L);
        circuit.setNumber("100000");
        testDID.setCircuit(circuit);
        when(didService.linkToCircuit(1L, "100000")).thenReturn(testDID);

        mockMvc.perform(put("/api/dids/1/link/100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitId").value(1))
                .andExpect(jsonPath("$.circuitNumber").value("100000"));
    }

    @Test
    void linkToCircuit_shouldReturn400_whenAlreadyLinked() throws Exception {
        when(didService.linkToCircuit(1L, "100000"))
                .thenThrow(new BusinessException("já está vinculado"));

        mockMvc.perform(put("/api/dids/1/link/100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unlinkFromCircuit_shouldReturn200() throws Exception {
        when(didService.unlinkFromCircuit(1L)).thenReturn(testDID);

        mockMvc.perform(put("/api/dids/1/unlink"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_shouldReturn204_whenFree() throws Exception {
        doNothing().when(didService).delete(1L);

        mockMvc.perform(delete("/api/dids/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn409_whenLinkedToCircuit() throws Exception {
        doThrow(new ConflictException("vinculado a um circuito"))
                .when(didService).delete(1L);

        mockMvc.perform(delete("/api/dids/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_shouldReturn404_whenNotExists() throws Exception {
        doThrow(new NotFoundException("DID não encontrado"))
                .when(didService).delete(99L);

        mockMvc.perform(delete("/api/dids/99"))
                .andExpect(status().isNotFound());
    }
}
