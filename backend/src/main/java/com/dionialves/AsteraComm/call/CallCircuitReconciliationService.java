package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CallCircuitReconciliationService {

    private final CallRepository callRepository;
    private final CdrRepository cdrRepository;
    private final CircuitRepository circuitRepository;
    private final ChannelParser channelParser;

    public List<OrphanCallDTO> findOrphanCalls() {
        List<Call> orphans = callRepository.findByCircuitIsNull();

        List<OrphanCallDTO> result = new ArrayList<>();
        for (Call call : orphans) {
            Optional<CdrRecord> cdrOpt = cdrRepository.findByUniqueId(call.getUniqueId());
            if (cdrOpt.isEmpty()) {
                result.add(new OrphanCallDTO(call.getId(), call.getUniqueId(), call.getCallDate(),
                        "CDR removido", null, false));
                continue;
            }
            String channelCode = channelParser.parse(cdrOpt.get().getChannel());
            if (channelCode.isEmpty()) {
                result.add(new OrphanCallDTO(call.getId(), call.getUniqueId(), call.getCallDate(),
                        "Canal vazio", null, false));
                continue;
            }
            Optional<Circuit> circuitOpt = circuitRepository.findByNumber(channelCode);
            if (circuitOpt.isPresent()) {
                result.add(new OrphanCallDTO(call.getId(), call.getUniqueId(), call.getCallDate(),
                        call.getDst(), channelCode, true));
            } else {
                result.add(new OrphanCallDTO(call.getId(), call.getUniqueId(), call.getCallDate(),
                        call.getDst(), channelCode, false));
            }
        }
        return result;
    }

    @Transactional
    public ReconciliationResultDTO reconcile() {
        List<OrphanCallDTO> orphans = findOrphanCalls();
        int linked = 0;
        int skipped = 0;
        List<String> skippedDetails = new ArrayList<>();

        for (OrphanCallDTO orphan : orphans) {
            if (!orphan.resolvable()) {
                skipped++;
                skippedDetails.add(orphan.uniqueId() + " — " + orphan.reasonOrDst());
                continue;
            }
            Optional<Call> callOpt = callRepository.findById(orphan.callId());
            Optional<Circuit> circuitOpt = circuitRepository.findByNumber(orphan.circuitCode());
            if (callOpt.isPresent() && circuitOpt.isPresent()) {
                Call call = callOpt.get();
                call.setCircuit(circuitOpt.get());
                callRepository.save(call);
                linked++;
            } else {
                skipped++;
                skippedDetails.add(orphan.uniqueId() + " — circuito ou call não encontrado");
            }
        }
        return new ReconciliationResultDTO(linked, skipped, skippedDetails);
    }
}