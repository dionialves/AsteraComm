package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrphanCallReportService {

    private final CallRepository callRepository;
    private final CdrRepository cdrRepository;
    private final CircuitRepository circuitRepository;
    private final ChannelParser channelParser;

    public List<OrphanCallReportDTO> findOrphanCalls(int month, int year) {
        List<Call> orphans = callRepository.findOrphanCallsByPeriod(month, year);
        List<OrphanCallReportDTO> result = new ArrayList<>();

        for (Call call : orphans) {
            Optional<CdrRecord> cdrOpt = cdrRepository.findByUniqueId(call.getUniqueId());
            String channel = cdrOpt.map(CdrRecord::getChannel).orElse(null);
            String circuitCode = null;
            boolean resolvable = false;

            if (channel != null && !channel.isBlank()) {
                circuitCode = channelParser.parse(channel);
                if (circuitCode != null && !circuitCode.isBlank()
                        && circuitRepository.findByNumber(circuitCode).isPresent()) {
                    resolvable = true;
                }
            }

            result.add(new OrphanCallReportDTO(
                    call.getId(),
                    call.getUniqueId(),
                    call.getCallDate(),
                    call.getDst(),
                    channel,
                    circuitCode,
                    resolvable
            ));
        }
        return result;
    }

    public long countOrphanCallsCurrentMonth() {
        LocalDate now = LocalDate.now();
        return callRepository.findOrphanCallsByPeriod(now.getMonthValue(), now.getYear()).size();
    }
}