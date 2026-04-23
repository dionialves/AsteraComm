package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
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
            // Tentativa 1: via channel → outbound (quem originou a ligação)
            String channelCode = channelParser.parse(cdr.getChannel());
            CallDirection direction = CallDirection.OUTBOUND;
            if (!channelCode.isEmpty()) {
                circuitRepository.findByNumber(channelCode).ifPresent(call::setCircuit);
            }
            // Tentativa 2: via dstChannel → inbound (quem atendeu a ligação)
            if (call.getCircuit() == null && cdr.getDstchannel() != null && !cdr.getDstchannel().isBlank()) {
                String dstChannelCode = channelParser.parse(cdr.getDstchannel());
                if (!dstChannelCode.isEmpty()) {
                    circuitRepository.findByNumber(dstChannelCode).ifPresent(c -> {
                        call.setCircuit(c);
                    });
                    if (call.getCircuit() != null) {
                        direction = CallDirection.INBOUND;
                    }
                }
            }
            call.setDirection(direction);
            callRepository.save(call);
            callCostingService.applyCosting(call, cdr.getDcontext());
            callRepository.save(call);
        }
    }
}
