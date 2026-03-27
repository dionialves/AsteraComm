package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.did.dto.DIDCircuitDTO;
import com.dionialves.AsteraComm.did.dto.DIDCreateDTO;
import com.dionialves.AsteraComm.did.dto.DIDResponseDTO;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.ConflictException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DIDService {

    private final DIDRepository didRepository;
    private final CircuitRepository circuitRepository;
    private final AsteriskProvisioningService asteriskProvisioningService;

    public Page<DIDResponseDTO> getAll(String search, String status, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        Page<DID> page;
        if ("IN_USE".equals(status) && hasSearch) {
            page = didRepository.findByCircuitIsNotNullAndNumberContaining(search, pageable);
        } else if ("IN_USE".equals(status)) {
            page = didRepository.findByCircuitIsNotNull(pageable);
        } else if ("FREE".equals(status) && hasSearch) {
            page = didRepository.findByCircuitIsNullAndNumberContaining(search, pageable);
        } else if ("FREE".equals(status)) {
            page = didRepository.findByCircuitIsNull(pageable);
        } else if (hasSearch) {
            page = didRepository.findByNumberContaining(search, pageable);
        } else {
            page = didRepository.findAll(pageable);
        }
        return page.map(this::toResponseDTO);
    }

    private DIDResponseDTO toResponseDTO(DID did) {
        String status = did.getCircuit() != null ? "IN_USE" : "FREE";
        DIDCircuitDTO circuit = did.getCircuit() != null
                ? new DIDCircuitDTO(did.getCircuit().getId(), did.getCircuit().getNumber())
                : null;
        return new DIDResponseDTO(did.getId(), did.getNumber(), status, circuit);
    }

    public Optional<DID> findById(Long id) {
        return didRepository.findById(id);
    }

    public List<DID> getFree() {
        return didRepository.findByCircuitIsNull();
    }

    public List<DID> getByCircuit(String circuitNumber) {
        return didRepository.findByCircuit_Number(circuitNumber);
    }

    @Transactional
    public DID create(DIDCreateDTO dto) {
        String number = dto.number();

        if (number == null || !number.matches("\\d+")) {
            throw new BusinessException("Número inválido: apenas dígitos são permitidos");
        }

        if (number.length() != 10) {
            throw new BusinessException("Número inválido: deve ter exatamente 10 dígitos");
        }

        if (didRepository.existsByNumber(number)) {
            throw new BusinessException("DID já cadastrado com este número");
        }

        DID did = new DID();
        did.setNumber(number);
        return didRepository.save(did);
    }

    @Transactional
    public DID linkToCircuit(Long id, String circuitNumber) {
        DID did = didRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DID não encontrado"));

        if (did.getCircuit() != null) {
            throw new BusinessException("DID já está vinculado a um circuito");
        }

        Circuit circuit = circuitRepository.findByNumber(circuitNumber)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado"));

        did.setCircuit(circuit);
        DID saved = didRepository.save(did);
        asteriskProvisioningService.provisionDid(saved, circuit);
        return saved;
    }

    @Transactional
    public DID unlinkFromCircuit(Long id) {
        DID did = didRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DID não encontrado"));

        if (did.getCircuit() == null) {
            throw new BusinessException("DID não está vinculado a nenhum circuito");
        }

        Circuit circuit = did.getCircuit();
        did.setCircuit(null);
        DID saved = didRepository.save(did);
        asteriskProvisioningService.deprovisionDid(did.getNumber(), circuit.getTrunkName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        DID did = didRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DID não encontrado"));

        if (did.getCircuit() != null) {
            throw new ConflictException("DID não pode ser removido pois está vinculado a um circuito");
        }

        didRepository.delete(did);
    }
}
