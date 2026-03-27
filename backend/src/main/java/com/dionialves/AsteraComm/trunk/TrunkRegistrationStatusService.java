package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.asterisk.provisioning.AmiService;
import lombok.RequiredArgsConstructor;
import org.asteriskjava.manager.response.CommandResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TrunkRegistrationStatusService {

    private final TrunkRepository trunkRepository;
    private final TrunkRegistrationStatusRepository statusRepository;
    private final AmiService amiService;

    @Scheduled(fixedRateString = "${asterisk.status.interval.ms}")
    public void updateRegistrationStatus() {
        List<Trunk> trunks = trunkRepository.findAll();
        if (trunks.isEmpty()) {
            return;
        }

        CommandResponse response = amiService.sendCommandWithResponse("pjsip show registrations");
        if (response == null || response.getResult() == null) {
            return;
        }

        List<String> lines = response.getResult();
        LocalDateTime now = LocalDateTime.now();

        statusRepository.deleteAll();

        for (Trunk trunk : trunks) {
            boolean registered = parseRegistered(lines, trunk.getName());
            statusRepository.save(new TrunkRegistrationStatus(trunk.getName(), registered, now));
        }
    }

    private boolean parseRegistered(List<String> lines, String trunkName) {
        for (String line : lines) {
            if (line.contains(trunkName + "/")) {
                return line.contains("Registered") && !line.contains("Unregistered");
            }
        }
        return false;
    }
}
