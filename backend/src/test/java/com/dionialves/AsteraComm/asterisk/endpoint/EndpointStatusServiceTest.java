package com.dionialves.AsteraComm.asterisk.endpoint;

import com.dionialves.AsteraComm.asterisk.provisioning.AmiService;
import org.asteriskjava.manager.response.CommandResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointStatusServiceTest {

    @Mock
    private EndpointFactory endpointFactory;

    @Mock
    private EndpointStatusRepository endpointStatusRepository;

    @Mock
    private AmiService amiService;

    @InjectMocks
    private EndpointStatusService endpointStatusService;

    private Endpoint testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpoint = new Endpoint();
        testEndpoint.setId("1001");
    }

    @Test
    void getStatusFromAsterisk_shouldDoNothing_whenAmiReturnsNull() {
        when(amiService.sendCommandWithResponse("pjsip show contacts")).thenReturn(null);

        endpointStatusService.getStatusFromAsterisk();

        verifyNoInteractions(endpointStatusRepository);
    }

    @Test
    void getStatusFromAsterisk_shouldSaveStatuses_whenAmiReturnsValidResponse() {
        CommandResponse response = mock(CommandResponse.class);
        when(response.getResult()).thenReturn(List.of(
                "  Contact:  1001/sip:1001@192.168.1.1:5060  Avail  1.5"
        ));
        when(amiService.sendCommandWithResponse("pjsip show contacts")).thenReturn(response);
        when(endpointFactory.getById("1001")).thenReturn(testEndpoint);

        endpointStatusService.getStatusFromAsterisk();

        verify(endpointStatusRepository).deleteAll();
        verify(endpointStatusRepository, times(1)).save(any(EndpointStatus.class));
    }

    @Test
    void saveEndpointStatus_shouldDeleteAllAndPersistEachStatus() {
        Endpoint ep = new Endpoint();
        ep.setId("1001");
        EndpointStatus status1 = new EndpointStatus(ep, true, "192.168.1.1", "1.5", null);
        EndpointStatus status2 = new EndpointStatus(ep, false, "192.168.1.2", "N/A", null);
        List<EndpointStatus> statuses = List.of(status1, status2);

        ReflectionTestUtils.invokeMethod(endpointStatusService, "saveEndpointStatus", statuses);

        verify(endpointStatusRepository).deleteAll();
        verify(endpointStatusRepository, times(2)).save(any(EndpointStatus.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEndpointStatus_shouldParseOnlineStatus_whenContactRegistered() {
        String line = "  Contact:  1001/sip:1001@192.168.1.1:5060  Avail  1.5";
        when(endpointFactory.getById("1001")).thenReturn(testEndpoint);

        List<EndpointStatus> result = (List<EndpointStatus>) ReflectionTestUtils.invokeMethod(
                endpointStatusService, "getEndpointStatus", List.of(line));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOnline()).isTrue();
        assertThat(result.get(0).getIp()).isEqualTo("192.168.1.1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEndpointStatus_shouldParseOfflineStatus_whenContactNotAvail() {
        String line = "  Contact:  1001/sip:1001@192.168.1.1:5060  Unavail  N/A";
        when(endpointFactory.getById("1001")).thenReturn(testEndpoint);

        List<EndpointStatus> result = (List<EndpointStatus>) ReflectionTestUtils.invokeMethod(
                endpointStatusService, "getEndpointStatus", List.of(line));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOnline()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEndpointStatus_shouldSkipLinesWithoutAtSign() {
        List<String> lines = List.of(
                "Contacts:",
                "==========",
                "  Name/Username  <something>  <Status>  <RTT(ms)>"
        );

        List<EndpointStatus> result = (List<EndpointStatus>) ReflectionTestUtils.invokeMethod(
                endpointStatusService, "getEndpointStatus", lines);

        assertThat(result).isEmpty();
        verifyNoInteractions(endpointFactory);
    }
}
