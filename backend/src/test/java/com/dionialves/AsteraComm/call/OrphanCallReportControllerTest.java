package com.dionialves.AsteraComm.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanCallReportControllerTest {

    @Mock
    private OrphanCallReportService reportService;

    @InjectMocks
    private OrphanCallReportController controller;

    @Test
    void link_postSetsModelAttributes() {
        OrphanCallReportDTO dto = new OrphanCallReportDTO(
                1L, "uid-001", LocalDateTime.of(2026, 3, 10, 14, 30),
                "4934000000", "PJSIP/123456-000045f0", null, "123456", true);
        when(reportService.linkOrphanCalls(3, 2026)).thenReturn(1);
        when(reportService.findOrphanCalls(3, 2026)).thenReturn(List.of(dto));
        when(reportService.countResolvable(3, 2026)).thenReturn(0L);

        controller.linkOrphanCalls(3, 2026, new ExtendedModelMap());

        verify(reportService).linkOrphanCalls(3, 2026);
        verify(reportService).findOrphanCalls(3, 2026);
        verify(reportService).countResolvable(3, 2026);
    }

    @Test
    void link_postReturnsTableFragment() {
        when(reportService.linkOrphanCalls(3, 2026)).thenReturn(0);
        when(reportService.findOrphanCalls(3, 2026)).thenReturn(List.of());
        when(reportService.countResolvable(3, 2026)).thenReturn(0L);

        String view = controller.linkOrphanCalls(3, 2026, new ExtendedModelMap());

        assertThat(view).isEqualTo("pages/reports/orphan-calls-table :: table");
    }
}