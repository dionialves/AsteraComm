package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanCallReportServiceTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private CdrRepository cdrRepository;
    @Mock
    private CircuitRepository circuitRepository;

    private ChannelParser channelParser;
    private OrphanCallReportService service;

    @BeforeEach
    void setUp() {
        channelParser = new ChannelParser();
        service = new OrphanCallReportService(callRepository, cdrRepository, circuitRepository, channelParser);
    }

    @Test
    void findOrphanCalls_returnsEmpty_whenNoOrphansForPeriod() {
        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of());

        List<OrphanCallReportDTO> result = service.findOrphanCalls(3, 2026);

        assertThat(result).isEmpty();
    }

    @Test
    void findOrphanCalls_returnsResolvable_whenChannelMatchesExistingCircuit() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("unique-001");
        orphan.setCallDate(LocalDateTime.of(2026, 3, 10, 14, 30));
        orphan.setDst("4934000000");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("unique-001");
        cdr.setChannel("PJSIP/123456-000045f0");

        Circuit circuit = new Circuit();
        circuit.setNumber("123456");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("unique-001")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("123456")).thenReturn(Optional.of(circuit));

        List<OrphanCallReportDTO> result = service.findOrphanCalls(3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isTrue();
        assertThat(result.get(0).circuitCode()).isEqualTo("123456");
    }

    @Test
    void findOrphanCalls_returnsNotResolvable_whenCircuitMissing() {
        Call orphan = new Call();
        orphan.setId(2L);
        orphan.setUniqueId("unique-002");
        orphan.setCallDate(LocalDateTime.of(2026, 3, 11, 9, 0));
        orphan.setDst("2199999999");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("unique-002");
        cdr.setChannel("PJSIP/999999-000045f0");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("unique-002")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        List<OrphanCallReportDTO> result = service.findOrphanCalls(3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isFalse();
        assertThat(result.get(0).circuitCode()).isEqualTo("999999");
    }

    @Test
    void findOrphanCalls_returnsNotResolvable_whenCdrMissing() {
        Call orphan = new Call();
        orphan.setId(3L);
        orphan.setUniqueId("unique-003");
        orphan.setCallDate(LocalDateTime.of(2026, 3, 12, 16, 0));
        orphan.setDst("1133333333");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("unique-003")).thenReturn(Optional.empty());

        List<OrphanCallReportDTO> result = service.findOrphanCalls(3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isFalse();
        assertThat(result.get(0).channel()).isNull();
    }

    @Test
    void countOrphanCallsCurrentMonth_returnsCountForCurrentMonth() {
        LocalDate now = LocalDate.now();
        when(callRepository.countOrphanCallsByPeriod(now.getMonthValue(), now.getYear())).thenReturn(3L);

        long count = service.countOrphanCallsCurrentMonth();

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void linkOrphanCalls_linksResolvableAndSkipsNonResolvable() {
        Call orphan1 = new Call();
        orphan1.setId(1L);
        orphan1.setUniqueId("uid-001");
        orphan1.setCallDate(LocalDateTime.of(2026, 3, 10, 14, 30));
        orphan1.setDst("4934000000");

        Call orphan2 = new Call();
        orphan2.setId(2L);
        orphan2.setUniqueId("uid-002");
        orphan2.setCallDate(LocalDateTime.of(2026, 3, 11, 10, 0));
        orphan2.setDst("2199999999");

        CdrRecord cdr1 = new CdrRecord();
        cdr1.setUniqueId("uid-001");
        cdr1.setChannel("PJSIP/123456-000045f0");

        CdrRecord cdr2 = new CdrRecord();
        cdr2.setUniqueId("uid-002");
        cdr2.setChannel("PJSIP/999999-000045f0");

        Circuit circuit = new Circuit();
        circuit.setNumber("123456");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan1, orphan2));
        when(cdrRepository.findByUniqueId("uid-001")).thenReturn(Optional.of(cdr1));
        when(cdrRepository.findByUniqueId("uid-002")).thenReturn(Optional.of(cdr2));
        when(circuitRepository.findByNumber("123456")).thenReturn(Optional.of(circuit));
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());
        when(callRepository.linkCircuitByUniqueId("uid-001", "123456")).thenReturn(1);

        int linked = service.linkOrphanCalls(3, 2026);

        assertThat(linked).isEqualTo(1);
        verify(callRepository).linkCircuitByUniqueId("uid-001", "123456");
    }

    @Test
    void linkOrphanCalls_returnsZero_whenNoResolvable() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("uid-001");
        orphan.setCallDate(LocalDateTime.of(2026, 3, 10, 14, 30));
        orphan.setDst("2199999999");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("uid-001");
        cdr.setChannel("PJSIP/999999-000045f0");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("uid-001")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        int linked = service.linkOrphanCalls(3, 2026);

        assertThat(linked).isZero();
    }

    @Test
    void countResolvable_returnsCorrectCount() {
        Call orphan1 = new Call();
        orphan1.setId(1L);
        orphan1.setUniqueId("uid-001");
        orphan1.setCallDate(LocalDateTime.of(2026, 3, 10, 14, 30));
        orphan1.setDst("4934000000");

        Call orphan2 = new Call();
        orphan2.setId(2L);
        orphan2.setUniqueId("uid-002");
        orphan2.setCallDate(LocalDateTime.of(2026, 3, 11, 10, 0));
        orphan2.setDst("2199999999");

        CdrRecord cdr1 = new CdrRecord();
        cdr1.setUniqueId("uid-001");
        cdr1.setChannel("PJSIP/123456-000045f0");

        CdrRecord cdr2 = new CdrRecord();
        cdr2.setUniqueId("uid-002");
        cdr2.setChannel("PJSIP/999999-000045f0");

        Circuit circuit = new Circuit();
        circuit.setNumber("123456");

        when(callRepository.findOrphanCallsByPeriod(3, 2026)).thenReturn(List.of(orphan1, orphan2));
        when(cdrRepository.findByUniqueId("uid-001")).thenReturn(Optional.of(cdr1));
        when(cdrRepository.findByUniqueId("uid-002")).thenReturn(Optional.of(cdr2));
        when(circuitRepository.findByNumber("123456")).thenReturn(Optional.of(circuit));
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        long count = service.countResolvable(3, 2026);

        assertThat(count).isEqualTo(1L);
    }
}