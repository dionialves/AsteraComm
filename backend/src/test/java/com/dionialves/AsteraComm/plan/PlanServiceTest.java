package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.dto.PlanCreateDTO;
import com.dionialves.AsteraComm.plan.dto.PlanSummaryDTO;
import com.dionialves.AsteraComm.plan.dto.PlanUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private Plan testPlan;

    @BeforeEach
    void setUp() {
        testPlan = new Plan();
        testPlan.setId(1L);
        testPlan.setName("Plano Básico");
        testPlan.setMonthlyPrice(new BigDecimal("99.90"));
        testPlan.setFixedLocal(new BigDecimal("0.0900"));
        testPlan.setFixedLongDistance(new BigDecimal("0.2100"));
        testPlan.setMobileLocal(new BigDecimal("0.4500"));
        testPlan.setMobileLongDistance(new BigDecimal("0.5500"));
        testPlan.setPackageType(PackageType.NONE);
    }

    private PlanCreateDTO nonePackageDTO() {
        return new PlanCreateDTO(
                "Plano Básico",
                new BigDecimal("99.90"),
                new BigDecimal("0.0900"),
                new BigDecimal("0.2100"),
                new BigDecimal("0.4500"),
                new BigDecimal("0.5500"),
                PackageType.NONE,
                null, null, null, null, null
        );
    }

    private PlanCreateDTO unifiedPackageDTO() {
        return new PlanCreateDTO(
                "Plano Unificado",
                new BigDecimal("149.90"),
                new BigDecimal("0.0900"),
                new BigDecimal("0.2100"),
                new BigDecimal("0.4500"),
                new BigDecimal("0.5500"),
                PackageType.UNIFIED,
                400, null, null, null, null
        );
    }

    private PlanCreateDTO perCategoryPackageDTO() {
        return new PlanCreateDTO(
                "Plano Categoria",
                new BigDecimal("129.90"),
                new BigDecimal("0.0900"),
                new BigDecimal("0.2100"),
                new BigDecimal("0.4500"),
                new BigDecimal("0.5500"),
                PackageType.PER_CATEGORY,
                null, 200, 200, 100, 0
        );
    }

    // --- getAll ---

    @Test
    void getAll_shouldDelegateToRepository() {
        Page<Plan> page = new PageImpl<>(List.of(testPlan));
        when(planRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Plan> result = planService.getAll("", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(planRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getAll_shouldReturnAll_whenActiveIsNull() {
        Page<Plan> page = new PageImpl<>(List.of(testPlan));
        when(planRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Plan> result = planService.getAll("", null, PageRequest.of(0, 10));

        verify(planRepository).findAll(PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAll_shouldReturnOnlyActive_whenActiveIsTrue() {
        Page<Plan> page = new PageImpl<>(List.of(testPlan));
        when(planRepository.findByActive(eq(true), any(Pageable.class))).thenReturn(page);

        Page<Plan> result = planService.getAll("", true, PageRequest.of(0, 10));

        verify(planRepository).findByActive(true, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAll_shouldReturnOnlyInactive_whenActiveIsFalse() {
        Page<Plan> page = new PageImpl<>(List.of());
        when(planRepository.findByActive(eq(false), any(Pageable.class))).thenReturn(page);

        Page<Plan> result = planService.getAll("", false, PageRequest.of(0, 10));

        verify(planRepository).findByActive(false, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void getAll_shouldFilterByActiveAndSearch_whenBothProvided() {
        Page<Plan> page = new PageImpl<>(List.of(testPlan));
        when(planRepository.findByActiveAndNameContainingIgnoreCase(eq(true), eq("básico"), any(Pageable.class))).thenReturn(page);

        Page<Plan> result = planService.getAll("básico", true, PageRequest.of(0, 10));

        verify(planRepository).findByActiveAndNameContainingIgnoreCase(true, "básico", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // --- findById ---

    @Test
    void findById_shouldReturnPlan_whenExists() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));

        Optional<Plan> result = planService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Plano Básico");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(planService.findById(99L)).isEmpty();
    }

    // --- create: NONE ---

    @Test
    void create_shouldSavePlan_withNonePackage() {
        when(planRepository.existsByName("Plano Básico")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        Plan result = planService.create(nonePackageDTO());

        verify(planRepository).save(argThat(p ->
                p.getName().equals("Plano Básico") &&
                p.getPackageType() == PackageType.NONE &&
                p.getPackageTotalMinutes() == null
        ));
        assertThat(result.getName()).isEqualTo("Plano Básico");
    }

    @Test
    void create_shouldSetActiveTrue_byDefault() {
        when(planRepository.existsByName("Plano Básico")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        Plan result = planService.create(nonePackageDTO());

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void create_shouldThrowBusinessException_whenNameAlreadyExists() {
        when(planRepository.existsByName("Plano Básico")).thenReturn(true);

        assertThatThrownBy(() -> planService.create(nonePackageDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já cadastrado");
    }

    // --- create: UNIFIED ---

    @Test
    void create_shouldSavePlan_withUnifiedPackage() {
        Plan unifiedPlan = new Plan();
        unifiedPlan.setId(2L);
        unifiedPlan.setName("Plano Unificado");
        unifiedPlan.setPackageType(PackageType.UNIFIED);
        unifiedPlan.setPackageTotalMinutes(400);

        when(planRepository.existsByName("Plano Unificado")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenReturn(unifiedPlan);

        Plan result = planService.create(unifiedPackageDTO());

        verify(planRepository).save(argThat(p ->
                p.getPackageType() == PackageType.UNIFIED &&
                p.getPackageTotalMinutes() == 400 &&
                p.getPackageFixedLocal() == null
        ));
        assertThat(result.getPackageTotalMinutes()).isEqualTo(400);
    }

    // --- create: PER_CATEGORY ---

    @Test
    void create_shouldSavePlan_withPerCategoryPackage() {
        Plan perCatPlan = new Plan();
        perCatPlan.setId(3L);
        perCatPlan.setName("Plano Categoria");
        perCatPlan.setPackageType(PackageType.PER_CATEGORY);
        perCatPlan.setPackageFixedLocal(200);
        perCatPlan.setPackageFixedLongDistance(200);
        perCatPlan.setPackageMobileLocal(100);
        perCatPlan.setPackageMobileLongDistance(0);

        when(planRepository.existsByName("Plano Categoria")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenReturn(perCatPlan);

        Plan result = planService.create(perCategoryPackageDTO());

        verify(planRepository).save(argThat(p ->
                p.getPackageType() == PackageType.PER_CATEGORY &&
                p.getPackageTotalMinutes() == null &&
                p.getPackageFixedLocal() == 200
        ));
        assertThat(result.getPackageFixedLocal()).isEqualTo(200);
    }

    // --- update ---

    @Test
    void update_shouldUpdateName_whenNewNameIsUnique() {
        PlanUpdateDTO dto = new PlanUpdateDTO("Plano Atualizado", null, null, null, null, null, null, null, null, null, null, null, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.existsByName("Plano Atualizado")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        planService.update(1L, dto);

        verify(planRepository).save(argThat(p -> p.getName().equals("Plano Atualizado")));
    }

    @Test
    void update_shouldThrowBusinessException_whenNewNameAlreadyExists() {
        PlanUpdateDTO dto = new PlanUpdateDTO("Nome Duplicado", null, null, null, null, null, null, null, null, null, null, null, null);
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.existsByName("Nome Duplicado")).thenReturn(true);

        assertThatThrownBy(() -> planService.update(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já cadastrado");
    }

    @Test
    void update_shouldThrowNotFoundException_whenPlanNotExists() {
        PlanUpdateDTO dto = new PlanUpdateDTO("X", null, null, null, null, null, null, null, null, null, null, null, null);
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.update(99L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Plano não encontrado");
    }

    @Test
    void update_shouldChangePackageType_toUnified() {
        PlanUpdateDTO dto = new PlanUpdateDTO(
                null, null, null, null, null, null,
                PackageType.UNIFIED, 300, null, null, null, null, null
        );
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        planService.update(1L, dto);

        verify(planRepository).save(argThat(p ->
                p.getPackageType() == PackageType.UNIFIED &&
                p.getPackageTotalMinutes() == 300
        ));
    }

    @Test
    void update_shouldUpdateActive_toFalse() {
        PlanUpdateDTO dto = new PlanUpdateDTO(
                null, null, null, null, null, null,
                null, null, null, null, null, null, false
        );
        testPlan.setActive(true);
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        Plan result = planService.update(1L, dto);

        verify(planRepository).save(argThat(p -> !p.isActive()));
    }

    @Test
    void update_shouldUpdateActive_toTrue() {
        PlanUpdateDTO dto = new PlanUpdateDTO(
                null, null, null, null, null, null,
                null, null, null, null, null, null, true
        );
        testPlan.setActive(false);
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        Plan result = planService.update(1L, dto);

        verify(planRepository).save(argThat(Plan::isActive));
    }

    // --- delete ---

    @Test
    void delete_shouldDeletePlan_whenNotLinkedToClient() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.isLinkedToClient(1L)).thenReturn(false);

        planService.delete(1L);

        verify(planRepository).delete(testPlan);
    }

    @Test
    void delete_shouldThrowBusinessException_whenLinkedToClient() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.isLinkedToClient(1L)).thenReturn(true);

        assertThatThrownBy(() -> planService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vinculado a um cliente");

        verify(planRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Plano não encontrado");
    }

    // --- findAllSummary ---

    @Test
    void findAllSummary_shouldDelegateToRepository() {
        var summaries = List.of(new PlanSummaryDTO(1L, "Plano Básico"));
        when(planRepository.findAllSummary()).thenReturn(summaries);

        List<PlanSummaryDTO> result = planService.findAllSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Plano Básico");
        verify(planRepository).findAllSummary();
    }

    @Test
    void findAllSummary_shouldReturnEmptyList_whenNoActivePlans() {
        when(planRepository.findAllSummary()).thenReturn(List.of());

        List<PlanSummaryDTO> result = planService.findAllSummary();

        assertThat(result).isEmpty();
    }
}
