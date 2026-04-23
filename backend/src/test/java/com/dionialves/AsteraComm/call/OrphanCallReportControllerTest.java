package com.dionialves.AsteraComm.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanCallReportControllerTest {

    @Mock
    private OrphanCallReportService reportService;

    @InjectMocks
    private OrphanCallReportController controller;

    @Test
    void table_returnsPaginatedModel() {
        OrphanCallReportDTO dto = new OrphanCallReportDTO(
                1L, "uid-001", LocalDateTime.of(2026, 3, 10, 14, 30),
                "4934000000", "PJSIP/123456-000045f0", null, "123456", true);
        Page<OrphanCallReportDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 50), 1);
        when(reportService.findOrphanCalls(eq(3), eq(2026), any())).thenReturn(page);
        when(reportService.countResolvable(3, 2026)).thenReturn(1L);

        ExtendedModelMap model = new ExtendedModelMap();
        controller.table(3, 2026, 0, 50, model);

        assertThat(model.containsAttribute("orphans")).isTrue();
        assertThat(model.get("orphans")).isInstanceOf(Page.class);
    }

    @Test
    void link_postSetsModelAttributes() {
        OrphanCallReportDTO dto = new OrphanCallReportDTO(
                1L, "uid-001", LocalDateTime.of(2026, 3, 10, 14, 30),
                "4934000000", "PJSIP/123456-000045f0", null, "123456", true);
        Page<OrphanCallReportDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 50), 1);
        when(reportService.linkOrphanCalls(3, 2026)).thenReturn(1);
        when(reportService.findOrphanCalls(eq(3), eq(2026), any())).thenReturn(page);
        when(reportService.countResolvable(3, 2026)).thenReturn(0L);

        controller.linkOrphanCalls(3, 2026, new ExtendedModelMap());

        verify(reportService).linkOrphanCalls(3, 2026);
        verify(reportService).findOrphanCalls(eq(3), eq(2026), any());
        verify(reportService).countResolvable(3, 2026);
    }

    @Test
    void link_postReturnsTableFragment() {
        Page<OrphanCallReportDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(reportService.linkOrphanCalls(3, 2026)).thenReturn(0);
        when(reportService.findOrphanCalls(eq(3), eq(2026), any())).thenReturn(emptyPage);
        when(reportService.countResolvable(3, 2026)).thenReturn(0L);

        String view = controller.linkOrphanCalls(3, 2026, new ExtendedModelMap());

        assertThat(view).isEqualTo("pages/reports/orphan-calls-table :: table");
    }
}