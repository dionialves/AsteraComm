package com.dionialves.AsteraComm.asterisk.endpoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dionialves.AsteraComm.asterisk.provisioning.AmiService;
import org.asteriskjava.manager.response.CommandResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EndpointStatusService {

    private final EndpointFactory endpointFactory;
    private final EndpointStatusRepository endpointStatusRepository;
    private final AmiService amiService;

    @Autowired
    public EndpointStatusService(EndpointFactory endpointFactory,
                                 EndpointStatusRepository endpointStatusRepository,
                                 AmiService amiService) {
        this.endpointFactory = endpointFactory;
        this.endpointStatusRepository = endpointStatusRepository;
        this.amiService = amiService;
    }

    @Scheduled(fixedRateString = "${asterisk.status.interval.ms}")
    public void getStatusFromAsterisk() {
        CommandResponse response = amiService.sendCommandWithResponse("pjsip show contacts");

        if (response == null) {
            System.err.println("Failed to get response from asterisk");
            return;
        }

        List<EndpointStatus> listEndpointStatus = this.getEndpointStatus(response.getResult());

        this.saveEndpointStatus(listEndpointStatus);
    }

    private List<EndpointStatus> getEndpointStatus(List<String> response) {

        List<EndpointStatus> listEndpointStatus = new ArrayList<>();

        for (String line : response) {

            if (!line.contains("@"))
                continue;

            try {

                String[] parts = line.trim().split("\\s+");

                String endpointId = parts[1].split("/")[0];
                Endpoint endpoint = endpointFactory.getById(endpointId);
                boolean status = line.contains("Avail");
                String ip = parts[1].split("/")[1].split("@")[1].split(":")[0];
                String rtt = parts[parts.length - 1];
                LocalDateTime now = LocalDateTime.now();

                listEndpointStatus.add(new EndpointStatus(endpoint, status, ip, rtt, now));
            } catch (Exception e) {
                System.err.println("Error processing line: " + line);
            }
        }

        return listEndpointStatus;
    }

    private void saveEndpointStatus(List<EndpointStatus> listEndpointStatus) {

        endpointStatusRepository.deleteAll();

        for (EndpointStatus endpointStatus : listEndpointStatus) {
            endpointStatusRepository.save(endpointStatus);
        }
    }
}
