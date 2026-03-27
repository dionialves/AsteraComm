package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.did.dto.DIDCreateDTO;
import com.dionialves.AsteraComm.did.dto.DIDResponseDTO;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.ConflictException;
import com.dionialves.AsteraComm.exception.NotFoundException;
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
class DIDServiceTest {

    @Mock
    private DIDRepository didRepository;

    @Mock
    private CircuitRepository circuitRepository;

    @Mock
    private AsteriskProvisioningService asteriskProvisioningService;

    @InjectMocks
    private DIDService didService;

    private DID testDID;
    private Circuit testCircuit;

    @BeforeEach
    void setUp() {
        testDID = new DID();
        testDID.setId(1L);
        testDID.setNumber("4933001234");
        testDID.setCircuit(null);

        testCircuit = new Circuit();
        testCircuit.setId(1L);
        testCircuit.setNumber("100000");
        testCircuit.setTrunkName("provedor1");
    }

    // --- getAll ---

    @Test
    void getAll_shouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findAll(pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(didRepository).findAll(pageable);
    }

    @Test
    void getAll_shouldReturnResponseDTOWithStatusFree_whenNoCircuit() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findAll(pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("", null, pageable);

        assertThat(result.getContent().get(0).status()).isEqualTo("FREE");
        assertThat(result.getContent().get(0).circuit()).isNull();
    }

    @Test
    void getAll_shouldReturnResponseDTOWithStatusInUse_whenCircuitLinked() {
        testDID.setCircuit(testCircuit);
        Pageable pageable = PageRequest.of(0, 20);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findAll(pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("", null, pageable);

        assertThat(result.getContent().get(0).status()).isEqualTo("IN_USE");
        assertThat(result.getContent().get(0).circuit()).isNotNull();
        assertThat(result.getContent().get(0).circuit().code()).isEqualTo("100000");
    }

    @Test
    void getAll_withInUseFilter_shouldQueryByCircuitIsNotNull() {
        Pageable pageable = PageRequest.of(0, 20);
        testDID.setCircuit(testCircuit);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findByCircuitIsNotNull(pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("", "IN_USE", pageable);

        assertThat(result.getContent().get(0).status()).isEqualTo("IN_USE");
        verify(didRepository).findByCircuitIsNotNull(pageable);
        verify(didRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAll_withFreeFilter_shouldQueryByCircuitIsNull() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findByCircuitIsNull(pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("", "FREE", pageable);

        assertThat(result.getContent().get(0).status()).isEqualTo("FREE");
        verify(didRepository).findByCircuitIsNull(pageable);
    }

    @Test
    void getAll_withInUseFilterAndSearch_shouldApplyBothFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        testDID.setCircuit(testCircuit);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findByCircuitIsNotNullAndNumberContaining("4933", pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("4933", "IN_USE", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(didRepository).findByCircuitIsNotNullAndNumberContaining("4933", pageable);
    }

    @Test
    void getAll_withFreeFilterAndSearch_shouldApplyBothFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DID> page = new PageImpl<>(List.of(testDID), pageable, 1);
        when(didRepository.findByCircuitIsNullAndNumberContaining("4933", pageable)).thenReturn(page);

        Page<DIDResponseDTO> result = didService.getAll("4933", "FREE", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(didRepository).findByCircuitIsNullAndNumberContaining("4933", pageable);
    }

    // --- findById ---

    @Test
    void findById_shouldReturnDID_whenExists() {
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));

        Optional<DID> result = didService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getNumber()).isEqualTo("4933001234");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        when(didRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<DID> result = didService.findById(99L);

        assertThat(result).isEmpty();
    }

    // --- getFree ---

    @Test
    void getFree_shouldReturnOnlyUnlinkedDIDs() {
        when(didRepository.findByCircuitIsNull()).thenReturn(List.of(testDID));

        List<DID> result = didService.getFree();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCircuit()).isNull();
        verify(didRepository).findByCircuitIsNull();
    }

    // --- create ---

    @Test
    void create_shouldSaveDID_whenNumberIsUniqueAndDigitsOnly() {
        DIDCreateDTO dto = new DIDCreateDTO("4933001234");
        when(didRepository.existsByNumber("4933001234")).thenReturn(false);
        when(didRepository.save(any(DID.class))).thenReturn(testDID);

        DID result = didService.create(dto);

        verify(didRepository).save(argThat(d -> d.getNumber().equals("4933001234")));
        assertThat(result.getNumber()).isEqualTo("4933001234");
    }

    @Test
    void create_shouldThrowBusinessException_whenNumberAlreadyExists() {
        DIDCreateDTO dto = new DIDCreateDTO("4933001234");
        when(didRepository.existsByNumber("4933001234")).thenReturn(true);

        assertThatThrownBy(() -> didService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DID já cadastrado");
    }

    @Test
    void create_shouldThrowBusinessException_whenNumberContainsNonDigits() {
        DIDCreateDTO dto = new DIDCreateDTO("(49) 3300-1234");

        assertThatThrownBy(() -> didService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("apenas dígitos");
    }

    @Test
    void create_shouldThrowBusinessException_whenNumberHasWrongLength() {
        DIDCreateDTO dtoShort = new DIDCreateDTO("123456789");
        DIDCreateDTO dtoLong  = new DIDCreateDTO("12345678901");

        assertThatThrownBy(() -> didService.create(dtoShort))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 10 dígitos");

        assertThatThrownBy(() -> didService.create(dtoLong))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 10 dígitos");
    }

    // --- linkToCircuit ---

    @Test
    void linkToCircuit_shouldSetCircuitAndProvisionDid_whenDIDFreeAndCircuitExists() {
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(didRepository.save(any(DID.class))).thenReturn(testDID);

        didService.linkToCircuit(1L, "100000");

        verify(didRepository).save(argThat(d -> testCircuit.equals(d.getCircuit())));
        verify(asteriskProvisioningService).provisionDid(any(DID.class), eq(testCircuit));
    }

    @Test
    void linkToCircuit_shouldThrowBusinessException_whenAlreadyLinked() {
        testDID.setCircuit(testCircuit);
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));

        assertThatThrownBy(() -> didService.linkToCircuit(1L, "100001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está vinculado");

        verifyNoInteractions(asteriskProvisioningService);
    }

    @Test
    void linkToCircuit_shouldThrowNotFoundException_whenCircuitNotExists() {
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> didService.linkToCircuit(1L, "999999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Circuito não encontrado");

        verifyNoInteractions(asteriskProvisioningService);
    }

    @Test
    void linkToCircuit_shouldThrowNotFoundException_whenDIDNotExists() {
        when(didRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> didService.linkToCircuit(99L, "100000"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("DID não encontrado");

        verifyNoInteractions(asteriskProvisioningService);
    }

    // --- unlinkFromCircuit ---

    @Test
    void unlinkFromCircuit_shouldClearCircuitAndDeprovisionDid_whenLinked() {
        testDID.setCircuit(testCircuit);
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));
        when(didRepository.save(any(DID.class))).thenReturn(testDID);

        didService.unlinkFromCircuit(1L);

        verify(didRepository).save(argThat(d -> d.getCircuit() == null));
        verify(asteriskProvisioningService).deprovisionDid("4933001234", "provedor1");
    }

    @Test
    void unlinkFromCircuit_shouldThrowBusinessException_whenNotLinked() {
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));

        assertThatThrownBy(() -> didService.unlinkFromCircuit(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está vinculado");

        verifyNoInteractions(asteriskProvisioningService);
    }

    @Test
    void unlinkFromCircuit_shouldThrowNotFoundException_whenDIDNotExists() {
        when(didRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> didService.unlinkFromCircuit(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("DID não encontrado");

        verifyNoInteractions(asteriskProvisioningService);
    }

    // --- delete ---

    @Test
    void delete_shouldDeleteDID_whenNotLinkedToCircuit() {
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));

        didService.delete(1L);

        verify(didRepository).delete(testDID);
    }

    @Test
    void delete_shouldThrowConflictException_whenLinkedToCircuit() {
        testDID.setCircuit(testCircuit);
        when(didRepository.findById(1L)).thenReturn(Optional.of(testDID));

        assertThatThrownBy(() -> didService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vinculado a um circuito");
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        when(didRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> didService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("DID não encontrado");
    }
}
