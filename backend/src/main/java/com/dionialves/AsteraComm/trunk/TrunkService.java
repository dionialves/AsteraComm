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

        Trunk trunk = new Trunk();
        trunk.setName(dto.name());
        trunk.setHost(dto.host());
        trunk.setUsername(dto.username());
        trunk.setPassword(dto.password());
        trunk.setPrefix(dto.prefix());
        Trunk saved = trunkRepository.save(trunk);

        asteriskProvisioningService.provisionTrunk(saved);

        return saved;
    }

    @Transactional
    public Trunk update(String name, TrunkCreateDTO dto) {
        Trunk trunk = trunkRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Tronco não encontrado"));

        trunk.setHost(dto.host());
        trunk.setUsername(dto.username());
        trunk.setPrefix(dto.prefix());
        if (dto.password() != null && !dto.password().isBlank()) {
            trunk.setPassword(dto.password());
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
}
