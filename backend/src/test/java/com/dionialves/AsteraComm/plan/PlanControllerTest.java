package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.dto.PlanCreateDTO;
import com.dionialves.AsteraComm.plan.dto.PlanSummaryDTO;
import com.dionialves.AsteraComm.plan.dto.PlanUpdateDTO;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController;

    private Plan testPlan;

    private static final String VALID_NONE_JSON = """
            {
              "name": "Plano Básico",
              "monthlyPrice": 99.90,
              "fixedLocal": 0.0900,
              "fixedLongDistance": 0.2100,
              "mobileLocal": 0.4500,
              "mobileLongDistance": 0.5500,
              "packageType": "NONE"
            }
            """;

    private static final String VALID_UNIFIED_JSON = """
            {
              "name": "Plano Unificado",
              "monthlyPrice": 149.90,
              "fixedLocal": 0.0900,
              "fixedLongDistance": 0.2100,
              "mobileLocal": 0.4500,
              "mobileLongDistance": 0.5500,
              "packageType": "UNIFIED",
              "packageTotalMinutes": 400
            }
            """;

    private static final String VALID_PER_CATEGORY_JSON = """
            {
              "name": "Plano Categoria",
              "monthlyPrice": 129.90,
              "fixedLocal": 0.0900,
              "fixedLongDistance": 0.2100,
              "mobileLocal": 0.4500,
              "mobileLongDistance": 0.5500,
              "packageType": "PER_CATEGORY",
              "packageFixedLocal": 200,
              "packageFixedLongDistance": 200,
              "packageMobileLocal": 100,
              "packageMobileLongDistance": 0
            }
            """;

    @BeforeEach
    void setUp() {
        testPlan = new Plan();
        testPlan.setId(1L);
        testPlan.setName("Plano Básico");
        testPlan.setMonthlyPrice(new BigDecimal("99.90"));
        testPlan.setFixedLocal(new BigDecimal("0.0900"));
        testPlan.setFixedLongDistance(new BigDecimal("0.2100"));
        testPlan.setMobileLocal(new BigDecimal("0.4500"));
        testPlan.setMobileLongDistance(new BigDecimal("0.5500"));
        testPlan.setPackageType(PackageType.NONE);

        mockMvc = MockMvcBuilders.standaloneSetup(planController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // --- GET /api/plans ---

    @Test
    void getAll_shouldReturn200_paginated() throws Exception {
        var page = new PageImpl<>(List.of(testPlan), PageRequest.of(0, 10), 1);
        when(planService.getAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Plano Básico"));
    }

    @Test
    void findAll_shouldFilterByActiveTrue() throws Exception {
        testPlan.setActive(true);
        var page = new PageImpl<>(List.of(testPlan), PageRequest.of(0, 10), 1);
        when(planService.getAll(any(), eq(true), any())).thenReturn(page);

        mockMvc.perform(get("/api/plans").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void findAll_shouldFilterByActiveFalse() throws Exception {
        Plan inactivePlan = new Plan();
        inactivePlan.setId(2L);
        inactivePlan.setName("Plano Inativo");
        inactivePlan.setActive(false);
        var page = new PageImpl<>(List.of(inactivePlan), PageRequest.of(0, 10), 1);
        when(planService.getAll(any(), eq(false), any())).thenReturn(page);

        mockMvc.perform(get("/api/plans").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    void update_shouldUpdateActiveField() throws Exception {
        testPlan.setActive(false);
        when(planService.update(eq(1L), any(PlanUpdateDTO.class))).thenReturn(testPlan);

        mockMvc.perform(put("/api/plans/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // --- GET /api/plans/{id} ---

    @Test
    void getById_shouldReturn200_whenExists() throws Exception {
        when(planService.findById(1L)).thenReturn(Optional.of(testPlan));

        mockMvc.perform(get("/api/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plano Básico"))
                .andExpect(jsonPath("$.packageType").value("NONE"));
    }

    @Test
    void getById_shouldReturn404_whenNotExists() throws Exception {
        when(planService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/plans/99"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/plans ---

    @Test
    void create_shouldReturn201_withNonePackage() throws Exception {
        when(planService.create(any(PlanCreateDTO.class))).thenReturn(testPlan);

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_NONE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plano Básico"));
    }

    @Test
    void create_shouldReturn201_withUnifiedPackage() throws Exception {
        Plan unifiedPlan = new Plan();
        unifiedPlan.setId(2L);
        unifiedPlan.setName("Plano Unificado");
        unifiedPlan.setPackageType(PackageType.UNIFIED);
        unifiedPlan.setPackageTotalMinutes(400);
        when(planService.create(any(PlanCreateDTO.class))).thenReturn(unifiedPlan);

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UNIFIED_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageTotalMinutes").value(400));
    }

    @Test
    void create_shouldReturn201_withPerCategoryPackage() throws Exception {
        Plan perCatPlan = new Plan();
        perCatPlan.setId(3L);
        perCatPlan.setName("Plano Categoria");
        perCatPlan.setPackageType(PackageType.PER_CATEGORY);
        perCatPlan.setPackageFixedLocal(200);
        when(planService.create(any(PlanCreateDTO.class))).thenReturn(perCatPlan);

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PER_CATEGORY_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageFixedLocal").value(200));
    }

    @Test
    void create_shouldReturn400_whenNameAlreadyExists() throws Exception {
        when(planService.create(any(PlanCreateDTO.class)))
                .thenThrow(new BusinessException("Plano já cadastrado"));

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_NONE_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenPackageTypeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Plano X",
                          "monthlyPrice": 99.90,
                          "fixedLocal": 0.09,
                          "fixedLongDistance": 0.21,
                          "mobileLocal": 0.45,
                          "mobileLongDistance": 0.55,
                          "packageType": "INVALID"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(planService);
    }

    @Test
    void create_shouldReturn400_whenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Plano X\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(planService);
    }

    @Test
    void create_shouldReturn400_whenUnifiedMissingTotalMinutes() throws Exception {
        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Plano X",
                          "monthlyPrice": 99.90,
                          "fixedLocal": 0.09,
                          "fixedLongDistance": 0.21,
                          "mobileLocal": 0.45,
                          "mobileLongDistance": 0.55,
                          "packageType": "UNIFIED"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(planService);
    }

    @Test
    void create_shouldReturn400_whenPerCategoryMissingField() throws Exception {
        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Plano X",
                          "monthlyPrice": 99.90,
                          "fixedLocal": 0.09,
                          "fixedLongDistance": 0.21,
                          "mobileLocal": 0.45,
                          "mobileLongDistance": 0.55,
                          "packageType": "PER_CATEGORY",
                          "packageFixedLocal": 200
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(planService);
    }

    @Test
    void create_shouldReturn400_whenNonePackageHasPackageFields() throws Exception {
        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Plano X",
                          "monthlyPrice": 99.90,
                          "fixedLocal": 0.09,
                          "fixedLongDistance": 0.21,
                          "mobileLocal": 0.45,
                          "mobileLongDistance": 0.55,
                          "packageType": "NONE",
                          "packageTotalMinutes": 400
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(planService);
    }

    // --- PUT /api/plans/{id} ---

    @Test
    void update_shouldReturn200_whenValid() throws Exception {
        when(planService.update(eq(1L), any(PlanUpdateDTO.class))).thenReturn(testPlan);

        mockMvc.perform(put("/api/plans/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Plano Atualizado\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn404_whenNotExists() throws Exception {
        when(planService.update(eq(99L), any(PlanUpdateDTO.class)))
                .thenThrow(new NotFoundException("Plano não encontrado"));

        mockMvc.perform(put("/api/plans/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400_whenNameAlreadyExists() throws Exception {
        when(planService.update(eq(1L), any(PlanUpdateDTO.class)))
                .thenThrow(new BusinessException("Plano já cadastrado"));

        mockMvc.perform(put("/api/plans/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Nome Duplicado\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/plans/{id} ---

    @Test
    void delete_shouldReturn204_whenNotLinkedToClient() throws Exception {
        doNothing().when(planService).delete(1L);

        mockMvc.perform(delete("/api/plans/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn400_whenLinkedToClient() throws Exception {
        doThrow(new BusinessException("vinculado a um cliente"))
                .when(planService).delete(1L);

        mockMvc.perform(delete("/api/plans/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldReturn404_whenNotExists() throws Exception {
        doThrow(new NotFoundException("Plano não encontrado"))
                .when(planService).delete(99L);

        mockMvc.perform(delete("/api/plans/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/plans/all ---

    @Test
    void getAllActive_shouldReturn200_withArray() throws Exception {
        var summaries = List.of(new PlanSummaryDTO(1L, "Plano Básico"));
        when(planService.findAllSummary()).thenReturn(summaries);

        mockMvc.perform(get("/api/plans/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Plano Básico"));
    }

    @Test
    void getAllActive_shouldReturn200_withEmptyArray_whenNoActivePlans() throws Exception {
        when(planService.findAllSummary()).thenReturn(List.of());

        mockMvc.perform(get("/api/plans/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
