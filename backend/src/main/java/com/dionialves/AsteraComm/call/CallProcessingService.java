package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.did.DIDRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CallProcessingService {

    private final CdrRepository cdrRepository;
    private final CallRepository callRepository;
    private final CallerIdParser callerIdParser;
    private final CallTypeClassifier callTypeClassifier;
    private final CircuitRepository circuitRepository;
    private final ChannelParser channelParser;
    private final CallCostingService callCostingService;
    private final DIDRepository didRepository;

    @Scheduled(fixedRateString = "${call.processing.interval.ms}")
    public void process() {
        List<CdrRecord> unprocessed = cdrRepository.findUnprocessed();
        for (CdrRecord cdr : unprocessed) {
            Call call = new Call();
            call.setUniqueId(cdr.getUniqueId());
            call.setCallDate(cdr.getCalldate());
            call.setCallerNumber(callerIdParser.parse(cdr.getClid()));
            call.setDst(cdr.getDst());
            call.setDurationSeconds(cdr.getDuration());
            call.setBillSeconds(cdr.getBillsec());
            call.setDisposition(cdr.getDisposition());
            call.setCallType(callTypeClassifier.classify(cdr.getDst()));
            call.setProcessedAt(LocalDateTime.now());
            String circuitCode = channelParser.parse(cdr.getChannel());
            if (!circuitCode.isEmpty()) {
                circuitRepository.findByNumber(circuitCode).ifPresent(call::setCircuit);
            }
            // Tentativa 2: association via dst -> DID (inbound)
            // Direction: se dst casou com DID → INBOUND; caso contrário → OUTBOUND
            CallDirection direction = CallDirection.OUTBOUND;
            if (call.getCircuit() == null && cdr.getDst() != null && !cdr.getDst().isBlank()) {
                var didOpt = didRepository.findByNumber(cdr.getDst()).filter(did -> did.getCircuit() != null);
                if (didOpt.isPresent()) {
                    call.setCircuit(didOpt.get().getCircuit());
                    direction = CallDirection.INBOUND;
                }
            }
            call.setDirection(direction);
            callRepository.save(call);
            callCostingService.applyCosting(call, cdr.getDcontext());
            callRepository.save(call);
        }
    }
}
