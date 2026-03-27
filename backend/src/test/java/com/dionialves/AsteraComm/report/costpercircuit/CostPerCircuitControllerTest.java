package com.dionialves.AsteraComm.report.costpercircuit;

import com.dionialves.AsteraComm.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CostPerCircuitControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CostPerCircuitService costPerCircuitService;

    @InjectMocks
    private CostPerCircuitController costPerCircuitController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(costPerCircuitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCallCostReport_shouldReturn200_withResults() throws Exception {
        List<CallCostReportDTO> data = List.of(
                new CallCostReportDTO("Cliente X", "Circuito A", 5, 10, new BigDecimal("1.50"))
        );
        when(costPerCircuitService.getReport(3, 2026, false)).thenReturn(data);

        mockMvc.perform(get("/api/reports/call-cost")
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Cliente X"))
                .andExpect(jsonPath("$[0].circuitName").value("Circuito A"))
                .andExpect(jsonPath("$[0].callCount").value(5))
                .andExpect(jsonPath("$[0].totalMinutes").value(10))
                .andExpect(jsonPath("$[0].totalCost").value(1.50));
    }

    @Test
    void getCallCostReport_shouldReturn200_withOnlyWithCostTrue() throws Exception {
        when(costPerCircuitService.getReport(3, 2026, true)).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/call-cost")
                        .param("month", "3")
                        .param("year", "2026")
                        .param("onlyWithCost", "true"))
                .andExpect(status().isOk());

        verify(costPerCircuitService).getReport(3, 2026, true);
    }

    @Test
    void getCallCostReport_shouldReturn200_withEmptyResult() throws Exception {
        when(costPerCircuitService.getReport(1, 2026, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/call-cost")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCallCostReport_shouldReturn400_whenMonthIsMissing() throws Exception {
        mockMvc.perform(get("/api/reports/call-cost")
                        .param("year", "2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCallCostReport_shouldReturn400_whenYearIsMissing() throws Exception {
        mockMvc.perform(get("/api/reports/call-cost")
                        .param("month", "3"))
                .andExpect(status().isBadRequest());
    }
}
