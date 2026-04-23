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

import org.springframework.transaction.annotation.Transactional;

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
            String dstChannel = cdrOpt.map(CdrRecord::getDstchannel).orElse(null);
            String circuitCode = null;
            boolean resolvable = false;

            // Tentativa 1: via channel (OUTBOUND)
            if (channel != null && !channel.isBlank()) {
                circuitCode = channelParser.parse(channel);
                if (circuitCode != null && !circuitCode.isBlank()
                        && circuitRepository.findByNumber(circuitCode).isPresent()) {
                    resolvable = true;
                }
            }

            // Tentativa 2: via dstChannel (INBOUND)
            if (!resolvable && dstChannel != null && !dstChannel.isBlank()) {
                circuitCode = channelParser.parse(dstChannel);
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
                    dstChannel,
                    circuitCode,
                    resolvable
            ));
        }
        return result;
    }

    public long countOrphanCallsCurrentMonth() {
        LocalDate now = LocalDate.now();
        return callRepository.countOrphanCallsByPeriod(now.getMonthValue(), now.getYear());
    }

    public long countResolvable(int month, int year) {
        return findOrphanCalls(month, year).stream().filter(OrphanCallReportDTO::resolvable).count();
    }

    @Transactional
    public int linkOrphanCalls(int month, int year) {
        List<OrphanCallReportDTO> orphans = findOrphanCalls(month, year);
        int linked = 0;
        for (OrphanCallReportDTO dto : orphans) {
            if (dto.resolvable() && dto.circuitCode() != null && !dto.circuitCode().isBlank()) {
                callRepository.linkCircuitByUniqueId(dto.uniqueId(), dto.circuitCode());
                linked++;
            }
        }
        return linked;
    }
}