package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrphanCallReportService {

    private final CallRepository callRepository;
    private final CdrRepository cdrRepository;
    private final CircuitRepository circuitRepository;
    private final ChannelParser channelParser;

    public Page<OrphanCallReportDTO> findOrphanCalls(int month, int year, Pageable pageable) {
        Page<Call> orphansPage = callRepository.findOrphanCallsByPeriod(month, year, pageable);
        List<OrphanCallReportDTO> dtos = buildReportDTOs(orphansPage.getContent(), month, year);
        return new PageImpl<>(dtos, pageable, orphansPage.getTotalElements());
    }

    public long countResolvable(int month, int year) {
        List<OrphanCallReportDTO> allDtos = findAllOrphanCallDTOs(month, year);
        return allDtos.stream().filter(OrphanCallReportDTO::resolvable).count();
    }

    @Transactional
    public int linkOrphanCalls(int month, int year) {
        List<OrphanCallReportDTO> allDtos = findAllOrphanCallDTOs(month, year);
        int linked = 0;
        for (OrphanCallReportDTO dto : allDtos) {
            if (dto.resolvable() && dto.circuitCode() != null && !dto.circuitCode().isBlank()) {
                callRepository.linkCircuitByUniqueId(dto.uniqueId(), dto.circuitCode());
                linked++;
            }
        }
        return linked;
    }

    private List<OrphanCallReportDTO> findAllOrphanCallDTOs(int month, int year) {
        List<Call> orphans = callRepository.findOrphanCallsByPeriod(month, year, Pageable.unpaged()).getContent();
        return buildReportDTOs(orphans, month, year);
    }

    private List<OrphanCallReportDTO> buildReportDTOs(List<Call> orphans, int month, int year) {
        if (orphans.isEmpty()) {
            return List.of();
        }

        List<String> uniqueIds = orphans.stream().map(Call::getUniqueId).distinct().toList();
        Map<String, CdrRecord> cdrByUniqueId = cdrRepository.findByUniqueIdIn(uniqueIds)
                .stream()
                .collect(Collectors.toMap(
                        CdrRecord::getUniqueId,
                        cdr -> cdr,
                        (first, second) -> first
                ));

        Set<String> allCircuitCodes = new HashSet<>();
        for (Call call : orphans) {
            CdrRecord cdr = cdrByUniqueId.get(call.getUniqueId());
            if (cdr != null) {
                String ch = cdr.getChannel();
                String dstCh = cdr.getDstchannel();
                if (ch != null && !ch.isBlank()) allCircuitCodes.add(channelParser.parse(ch));
                if (dstCh != null && !dstCh.isBlank()) allCircuitCodes.add(channelParser.parse(dstCh));
            }
        }
        allCircuitCodes.removeIf(String::isBlank);

        Map<String, Circuit> circuitByNumber = circuitRepository.findByNumberIn(List.copyOf(allCircuitCodes))
                .stream()
                .collect(Collectors.toMap(Circuit::getNumber, c -> c, (first, second) -> first));

        List<OrphanCallReportDTO> result = new ArrayList<>();
        for (Call call : orphans) {
            CdrRecord cdr = cdrByUniqueId.get(call.getUniqueId());
            String channel = (cdr != null) ? cdr.getChannel() : null;
            String dstChannel = (cdr != null) ? cdr.getDstchannel() : null;
            String circuitCode = null;
            boolean resolvable = false;

            if (channel != null && !channel.isBlank()) {
                circuitCode = channelParser.parse(channel);
                if (circuitCode != null && !circuitCode.isBlank() && circuitByNumber.containsKey(circuitCode)) {
                    resolvable = true;
                }
            }

            if (!resolvable && dstChannel != null && !dstChannel.isBlank()) {
                circuitCode = channelParser.parse(dstChannel);
                if (circuitCode != null && !circuitCode.isBlank() && circuitByNumber.containsKey(circuitCode)) {
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
}