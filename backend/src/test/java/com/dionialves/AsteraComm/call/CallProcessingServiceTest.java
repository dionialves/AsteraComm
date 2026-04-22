package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.did.DID;
import com.dionialves.AsteraComm.did.DIDRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallProcessingServiceTest {

    @Mock
    private CdrRepository cdrRepository;

    @Mock
    private CallRepository callRepository;

    @Mock
    private CallerIdParser callerIdParser;

    @Mock
    private CallTypeClassifier callTypeClassifier;

    @Mock
    private CircuitRepository circuitRepository;

    @Mock
    private ChannelParser channelParser;

    @Mock
    private CallCostingService callCostingService;

    @Mock
    private DIDRepository didRepository;

    @InjectMocks
    private CallProcessingService callProcessingService;

    private CdrRecord buildCdr(String uniqueId, String disposition, String channel) {
        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId(uniqueId);
        cdr.setCalldate(LocalDateTime.of(2026, 3, 12, 10, 0, 0));
        cdr.setClid("\"Cliente\" <11933334444>");
        cdr.setSrc("11933334444");
        cdr.setDst("1133334444");
        cdr.setChannel(channel);
        cdr.setDuration(120);
        cdr.setBillsec(115);
        cdr.setDisposition(disposition);
        return cdr;
    }

    @Test
    void process_shouldCreateCall_forAnsweredCdr() {
        CdrRecord cdr = buildCdr("1000.1", "ANSWERED", "PJSIP/4933401714-000045f0");
        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse("\"Cliente\" <11933334444>")).thenReturn("11933334444");
        when(callTypeClassifier.classify("1133334444")).thenReturn(CallType.FIXED_LONG_DISTANCE);
        when(channelParser.parse("PJSIP/4933401714-000045f0")).thenReturn("4933401714");
        when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.empty());

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        Call saved = captor.getAllValues().get(0);
        assertThat(saved.getUniqueId()).isEqualTo("1000.1");
        assertThat(saved.getCallerNumber()).isEqualTo("11933334444");
        assertThat(saved.getDst()).isEqualTo("1133334444");
        assertThat(saved.getCallType()).isEqualTo(CallType.FIXED_LONG_DISTANCE);
        assertThat(saved.getDisposition()).isEqualTo("ANSWERED");
        assertThat(saved.getDurationSeconds()).isEqualTo(120);
        assertThat(saved.getBillSeconds()).isEqualTo(115);
    }

    @Test
    void process_shouldAssociateCircuit_whenChannelMatchesExistingCircuit() {
        Circuit circuit = new Circuit();
        circuit.setNumber("4933401714");

        CdrRecord cdr = buildCdr("1000.2", "ANSWERED", "PJSIP/4933401714-000045f0");
        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
        when(channelParser.parse("PJSIP/4933401714-000045f0")).thenReturn("4933401714");
        when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.of(circuit));

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCircuit()).isEqualTo(circuit);
    }

    @Test
    void process_shouldLeaveCircuitNull_whenChannelCodeNotFound() {
        CdrRecord cdr = buildCdr("1000.3", "ANSWERED", "PJSIP/9999999999-000045f0");
        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
        when(channelParser.parse("PJSIP/9999999999-000045f0")).thenReturn("9999999999");
        when(circuitRepository.findByNumber("9999999999")).thenReturn(Optional.empty());

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCircuit()).isNull();
    }

    @Test
    void process_shouldLeaveCircuitNull_whenChannelIsNull() {
        CdrRecord cdr = buildCdr("1000.4", "ANSWERED", null);
        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
        when(channelParser.parse(null)).thenReturn("");

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCircuit()).isNull();
        verify(circuitRepository, never()).findById(any());
    }

    @Test
    void process_shouldCreateCall_forAllDispositions() {
        CdrRecord noAnswer = buildCdr("1000.5", "NO ANSWER", "PJSIP/4933401714-0001");
        CdrRecord busy = buildCdr("1000.6", "BUSY", "PJSIP/4933401714-0002");
        CdrRecord failed = buildCdr("1000.7", "FAILED", "PJSIP/4933401714-0003");
        when(cdrRepository.findUnprocessed()).thenReturn(List.of(noAnswer, busy, failed));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LONG_DISTANCE);
        when(channelParser.parse(any())).thenReturn("4933401714");
        when(circuitRepository.findByNumber(any())).thenReturn(Optional.empty());

        callProcessingService.process();

        verify(callRepository, times(6)).save(any(Call.class));
    }

    @Test
    void process_shouldNotSaveAnything_whenAllCdrsAlreadyProcessed() {
        when(cdrRepository.findUnprocessed()).thenReturn(List.of());

        callProcessingService.process();

        verify(callRepository, never()).save(any());
    }

    @Test
    void process_shouldAssociateCircuitViaDstDid_whenInboundCall() {
        Circuit circuit = new Circuit();
        circuit.setNumber("4933401714");

        DID did = new DID();
        did.setNumber("5511999990000");
        did.setCircuit(circuit);

        CdrRecord cdr = buildCdr("1000.10", "ANSWERED", "PJSIP/operadora-0001");
        cdr.setDst("5511999990000");

        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
        when(channelParser.parse("PJSIP/operadora-0001")).thenReturn("operadora");
        when(circuitRepository.findByNumber("operadora")).thenReturn(Optional.empty());
        when(didRepository.findByNumber("5511999990000")).thenReturn(Optional.of(did));

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCircuit()).isEqualTo(circuit);
    }

    @Test
    void process_shouldLeaveCircuitNull_whenDidNotFound() {
        CdrRecord cdr = buildCdr("1000.11", "ANSWERED", "PJSIP/operadora-0001");
        cdr.setDst("00000000000");

        when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
        when(callerIdParser.parse(any())).thenReturn("11933334444");
        when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
        when(channelParser.parse("PJSIP/operadora-0001")).thenReturn("operadora");
        when(circuitRepository.findByNumber("operadora")).thenReturn(Optional.empty());
        when(didRepository.findByNumber("00000000000")).thenReturn(Optional.empty());

        callProcessingService.process();

        ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
        verify(callRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCircuit()).isNull();
    }
}
