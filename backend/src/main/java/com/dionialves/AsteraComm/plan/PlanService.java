package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.dto.PlanCreateDTO;
import com.dionialves.AsteraComm.plan.dto.PlanSummaryDTO;
import com.dionialves.AsteraComm.plan.dto.PlanUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PlanService {

    private final PlanRepository planRepository;

    public Page<Plan> getAll(String search, Boolean active, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        if (active == null) {
            return hasSearch
                    ? planRepository.findByNameContainingIgnoreCase(search, pageable)
                    : planRepository.findAll(pageable);
        }
        return hasSearch
                ? planRepository.findByActiveAndNameContainingIgnoreCase(active, search, pageable)
                : planRepository.findByActive(active, pageable);
    }

    public List<PlanSummaryDTO> findAllSummary() {
        return planRepository.findAllSummary();
    }

    public Optional<Plan> findById(Long id) {
        return planRepository.findById(id);
    }

    @Transactional
    public Plan create(PlanCreateDTO dto) {
        if (planRepository.existsByName(dto.name())) {
            throw new BusinessException("Plano já cadastrado com este nome");
        }

        Plan plan = new Plan();
        plan.setName(dto.name());
        plan.setMonthlyPrice(dto.monthlyPrice());
        plan.setFixedLocal(dto.fixedLocal());
        plan.setFixedLongDistance(dto.fixedLongDistance());
        plan.setMobileLocal(dto.mobileLocal());
        plan.setMobileLongDistance(dto.mobileLongDistance());
        plan.setPackageType(dto.packageType());
        plan.setPackageTotalMinutes(dto.packageTotalMinutes());
        plan.setPackageFixedLocal(dto.packageFixedLocal());
        plan.setPackageFixedLongDistance(dto.packageFixedLongDistance());
        plan.setPackageMobileLocal(dto.packageMobileLocal());
        plan.setPackageMobileLongDistance(dto.packageMobileLongDistance());

        return planRepository.save(plan);
    }

    @Transactional
    public Plan update(Long id, PlanUpdateDTO dto) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));

        if (dto.name() != null && !dto.name().equals(plan.getName())) {
            if (planRepository.existsByName(dto.name())) {
                throw new BusinessException("Plano já cadastrado com este nome");
            }
            plan.setName(dto.name());
        }

        if (dto.monthlyPrice() != null)      plan.setMonthlyPrice(dto.monthlyPrice());
        if (dto.fixedLocal() != null)         plan.setFixedLocal(dto.fixedLocal());
        if (dto.fixedLongDistance() != null)  plan.setFixedLongDistance(dto.fixedLongDistance());
        if (dto.mobileLocal() != null)        plan.setMobileLocal(dto.mobileLocal());
        if (dto.mobileLongDistance() != null) plan.setMobileLongDistance(dto.mobileLongDistance());

        if (dto.packageType() != null) {
            plan.setPackageType(dto.packageType());
            plan.setPackageTotalMinutes(dto.packageTotalMinutes());
            plan.setPackageFixedLocal(dto.packageFixedLocal());
            plan.setPackageFixedLongDistance(dto.packageFixedLongDistance());
            plan.setPackageMobileLocal(dto.packageMobileLocal());
            plan.setPackageMobileLongDistance(dto.packageMobileLongDistance());
        }

        if (dto.active() != null) plan.setActive(dto.active());

        return planRepository.save(plan);
    }

    @Transactional
    public void delete(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));

        if (planRepository.isLinkedToClient(id)) {
            throw new BusinessException("Plano não pode ser removido pois está vinculado a um cliente");
        }

        planRepository.delete(plan);
    }
}
