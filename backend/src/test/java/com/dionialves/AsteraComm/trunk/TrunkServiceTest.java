package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO;
import com.dionialves.AsteraComm.trunk.dto.TrunkCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrunkServiceTest {

    @Mock
    private TrunkRepository trunkRepository;

    @Mock
    private TrunkRegistrationStatusRepository trunkRegistrationStatusRepository;

    @Mock
    private AsteriskProvisioningService asteriskProvisioningService;

    @InjectMocks
    private TrunkService trunkService;

    private Trunk testTrunk;

    @BeforeEach
    void setUp() {
        testTrunk = new Trunk();
        testTrunk.setId(1L);
        testTrunk.setName("provedor1");
        testTrunk.setHost("sip.provedor1.com.br");
        testTrunk.setUsername("user123");
        testTrunk.setPassword("senha123");
    }

    @Test
    void getAll_shouldDelegateToRepository() {
        Page<TrunkProjection> page = new PageImpl<>(List.of());
        when(trunkRepository.findAllTrunks(anyString(), any(Pageable.class))).thenReturn(page);

        Page<TrunkProjection> result = trunkService.getAll("", PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(trunkRepository).findAllTrunks("", PageRequest.of(0, 10));
    }

    @Test
    void findByName_shouldReturnTrunk_whenExists() {
        when(trunkRepository.findByName("provedor1")).thenReturn(Optional.of(testTrunk));

        Optional<Trunk> result = trunkService.findByName("provedor1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("provedor1");
    }

    @Test
    void findByName_shouldReturnEmpty_whenNotExists() {
        when(trunkRepository.findByName("inexistente")).thenReturn(Optional.empty());

        Optional<Trunk> result = trunkService.findByName("inexistente");

        assertThat(result).isEmpty();
    }

    @Test
    void create_shouldSaveTrunkAndCallProvision() {
        TrunkCreateDTO dto = new TrunkCreateDTO("provedor2", "sip.prov2.com", "user2", "pass2", null);
        when(trunkRepository.existsByName("provedor2")).thenReturn(false);
        when(trunkRepository.save(any(Trunk.class))).thenReturn(testTrunk);

        trunkService.create(dto);

        verify(trunkRepository).save(argThat(t ->
                t.getName().equals("provedor2")
                && t.getHost().equals("sip.prov2.com")
                && t.getUsername().equals("user2")
                && t.getPassword().equals("pass2")));
        verify(asteriskProvisioningService).provisionTrunk(any(Trunk.class));
    }

    @Test
    void create_shouldThrowBusinessException_whenNameAlreadyExists() {
        TrunkCreateDTO dto = new TrunkCreateDTO("provedor1", "sip.prov.com", "user", "pass", null);
        when(trunkRepository.existsByName("provedor1")).thenReturn(true);

        assertThatThrownBy(() -> trunkService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tronco já existe");
    }

    @Test
    void update_shouldUpdateFieldsAndCallReprovision() {
        TrunkCreateDTO dto = new TrunkCreateDTO("provedor1", "novo.host.com", "newuser", "newpass", null);
        when(trunkRepository.findByName("provedor1")).thenReturn(Optional.of(testTrunk));
        when(trunkRepository.save(any(Trunk.class))).thenReturn(testTrunk);

        trunkService.update("provedor1", dto);

        verify(trunkRepository).save(argThat(t ->
                t.getHost().equals("novo.host.com")
                && t.getUsername().equals("newuser")
                && t.getPassword().equals("newpass")));
        verify(asteriskProvisioningService).reprovisionTrunk(any(Trunk.class));
    }

    @Test
    void update_shouldNotChangePassword_whenPasswordIsBlank() {
        TrunkCreateDTO dto = new TrunkCreateDTO("provedor1", "novo.host.com", "newuser", "", null);
        when(trunkRepository.findByName("provedor1")).thenReturn(Optional.of(testTrunk));
        when(trunkRepository.save(any(Trunk.class))).thenReturn(testTrunk);

        trunkService.update("provedor1", dto);

        verify(trunkRepository).save(argThat(t -> t.getPassword().equals("senha123")));
    }

    @Test
    void update_shouldThrowNotFoundException_whenNotExists() {
        TrunkCreateDTO dto = new TrunkCreateDTO("inexistente", "host.com", "user", "pass", null);
        when(trunkRepository.findByName("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trunkService.update("inexistente", dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Tronco não encontrado");
    }

    @Test
    void delete_shouldCleanStatusCallDeprovisionAndDeleteTrunk() {
        when(trunkRepository.findByName("provedor1")).thenReturn(Optional.of(testTrunk));

        trunkService.delete("provedor1");

        verify(trunkRegistrationStatusRepository).deleteByTrunkName("provedor1");
        verify(asteriskProvisioningService).deprovisionTrunk(testTrunk);
        verify(trunkRepository).delete(testTrunk);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        when(trunkRepository.findByName("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trunkService.delete("inexistente"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Tronco não encontrado");
    }

    // --- findAll ---

    @Test
    void findAll_shouldDelegateToRepository() {
        var summaries = List.of(new TrunkSummaryDTO("provedor1"));
        when(trunkRepository.findAllSummary()).thenReturn(summaries);

        List<TrunkSummaryDTO> result = trunkService.findAllSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("provedor1");
        verify(trunkRepository).findAllSummary();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoTrunks() {
        when(trunkRepository.findAllSummary()).thenReturn(List.of());

        List<TrunkSummaryDTO> result = trunkService.findAllSummary();

        assertThat(result).isEmpty();
    }
}
