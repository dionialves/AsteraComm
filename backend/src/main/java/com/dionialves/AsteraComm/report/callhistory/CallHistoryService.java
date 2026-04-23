package com.dionialves.AsteraComm.report.callhistory;

import com.dionialves.AsteraComm.call.Call;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CallHistoryService {

    private final CircuitRepository circuitRepository;
    private final CallRepository    callRepository;

    public CallHistoryResultDTO getHistory(String circuitNumber, int month, int year) {
        Circuit circuit = circuitRepository.findByNumber(circuitNumber)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado: " + circuitNumber));

        List<Call> calls = callRepository.findByCircuitNumberAndPeriod(circuitNumber, month, year);

        List<CallHistoryLineDTO> lines = calls.stream()
                .map(this::toLineDTO)
                .toList();

        int totalCalls = lines.size();
        int totalBillSec = lines.stream().mapToInt(CallHistoryLineDTO::billSeconds).sum();
        BigDecimal totalMinutes = BigDecimal.valueOf(Math.ceil(totalBillSec / 30.0))
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);

        return new CallHistoryResultDTO(circuitNumber, month, year, totalCalls, totalMinutes, lines);
    }

    private CallHistoryLineDTO toLineDTO(Call call) {
        int billSec = call.getBillSeconds();
        Integer durationSec = billSec > 3 ? call.getDurationSeconds() : null;
        return new CallHistoryLineDTO(
                call.getUniqueId(),
                call.getCallDate(),
                call.getCallerNumber(),
                call.getDst(),
                call.getDisposition(),
                call.getCallType(),
                call.getDirection(),
                billSec,
                durationSec
        );
    }
}