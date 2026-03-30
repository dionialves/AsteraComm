package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.trunk.dto.TrunkCreateDTO;
import com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TrunkService {

    private final TrunkRepository trunkRepository;
    private final TrunkRegistrationStatusRepository trunkRegistrationStatusRepository;
    private final AsteriskProvisioningService asteriskProvisioningService;

    public Page<TrunkProjection> getAll(String search, Pageable pageable) {
        return trunkRepository.findAllTrunks(search, pageable);
    }

    public List<TrunkSummaryDTO> findAllSummary() {
        return trunkRepository.findAllSummary();
    }

    public Optional<Trunk> findByName(String name) {
        return trunkRepository.findByName(name);
    }

    @Transactional
    public Trunk create(TrunkCreateDTO dto) {
        if (trunkRepository.existsByName(dto.name())) {
            throw new BusinessException("Tronco já existe com este nome");
        }

        TrunkAuthType authType = dto.authType() != null ? dto.authType() : TrunkAuthType.CREDENTIAL;
        validate(dto, authType);

        Trunk trunk = new Trunk();
        trunk.setName(dto.name());
        trunk.setHost(dto.host());
        trunk.setAuthType(authType);
        trunk.setPrefix(dto.prefix());

        if (authType == TrunkAuthType.CREDENTIAL) {
            trunk.setUsername(dto.username());
            trunk.setPassword(dto.password());
        } else {
            trunk.setIdentifyMatch(dto.identifyMatch());
        }

        Trunk saved = trunkRepository.save(trunk);
        asteriskProvisioningService.provisionTrunk(saved);
        return saved;
    }

    @Transactional
    public Trunk update(String name, TrunkCreateDTO dto) {
        Trunk trunk = trunkRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Tronco não encontrado"));

        trunk.setHost(dto.host());
        trunk.setPrefix(dto.prefix());

        if (trunk.getAuthType() == TrunkAuthType.CREDENTIAL) {
            trunk.setUsername(dto.username());
            if (dto.password() != null && !dto.password().isBlank()) {
                trunk.setPassword(dto.password());
            }
        } else {
            if (dto.identifyMatch() != null && !dto.identifyMatch().isBlank()) {
                trunk.setIdentifyMatch(dto.identifyMatch());
            }
        }

        Trunk saved = trunkRepository.save(trunk);
        asteriskProvisioningService.reprovisionTrunk(saved);
        return saved;
    }

    @Transactional
    public void delete(String name) {
        Trunk trunk = trunkRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Tronco não encontrado"));

        trunkRegistrationStatusRepository.deleteByTrunkName(name);
        asteriskProvisioningService.deprovisionTrunk(trunk);
        trunkRepository.delete(trunk);
    }

    private void validate(TrunkCreateDTO dto, TrunkAuthType authType) {
        if (authType == TrunkAuthType.CREDENTIAL) {
            if (dto.username() == null || dto.username().isBlank()) {
                throw new BusinessException("Tronco do tipo Login/Senha requer usuário");
            }
            if (dto.password() == null || dto.password().isBlank()) {
                throw new BusinessException("Tronco do tipo Login/Senha requer senha");
            }
        } else {
            if (dto.identifyMatch() == null || dto.identifyMatch().isBlank()) {
                throw new BusinessException("Tronco por IP requer o campo identifyMatch");
            }
        }
    }
}
