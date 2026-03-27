package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.circuit.dto.CircuitCreateDTO;
import com.dionialves.AsteraComm.circuit.dto.CircuitSummaryDTO;
import com.dionialves.AsteraComm.customer.Customer;
import com.dionialves.AsteraComm.customer.CustomerRepository;
import com.dionialves.AsteraComm.did.DIDRepository;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.Plan;
import com.dionialves.AsteraComm.plan.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CircuitService {

    private final CircuitRepository circuitRepository;
    private final DIDRepository didRepository;
    private final CallRepository callRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final AsteriskProvisioningService asteriskProvisioningService;

    public Page<CircuitProjection> getAll(String search, Boolean online, Boolean active, Pageable pageable) {
        return circuitRepository.findAllCircuits(search, online, active, pageable);
    }

    public List<CircuitSummaryDTO> findAllSummary() {
        return circuitRepository.findAllSummary();
    }

    public Optional<Circuit> findByNumber(String number) {
        return circuitRepository.findByNumber(number);
    }

    @Transactional
    public Circuit create(CircuitCreateDTO dto) {
        String code = circuitRepository.findMaxCode()
                .map(max -> String.valueOf(Long.parseLong(max) + 1))
                .orElse("100000");

        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        Circuit circuit = new Circuit();
        circuit.setNumber(code);
        circuit.setPassword(dto.password());
        circuit.setTrunkName(dto.trunkName());
        circuit.setCustomer(customer);
        circuit.setPlan(resolvePlan(dto.planId()));
        circuit.setActive(dto.active() != null ? dto.active() : true);
        Circuit saved = circuitRepository.save(circuit);

        asteriskProvisioningService.provision(saved);

        return saved;
    }

    @Transactional
    public Circuit update(String number, CircuitCreateDTO dto) {
        Circuit circuit = circuitRepository.findByNumber(number)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado"));

        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        String previousTrunkName = circuit.getTrunkName();

        circuit.setPassword(dto.password());
        circuit.setTrunkName(dto.trunkName());
        circuit.setCustomer(customer);
        circuit.setPlan(resolvePlan(dto.planId()));
        if (dto.active() != null)
            circuit.setActive(dto.active());
        Circuit saved = circuitRepository.save(circuit);

        asteriskProvisioningService.reprovision(saved, previousTrunkName);

        return saved;
    }

    @Transactional
    public Optional<Circuit> delete(String number) {
        Circuit circuit = circuitRepository.findByNumber(number)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado"));

        if (didRepository.existsByCircuit(circuit)) {
            throw new BusinessException("Desvincule os DIDs antes de excluir o circuito");
        }

        if (callRepository.existsByCircuitNumber(number)) {
            throw new BusinessException("Não é possível excluir um circuito com chamadas associadas.");
        }

        asteriskProvisioningService.deprovision(circuit);
        circuitRepository.delete(circuit);
        return Optional.empty();
    }

    private Plan resolvePlan(Long planId) {
        if (planId == null)
            throw new BusinessException("Plano é obrigatório para o circuito");
        return planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
    }
}
