package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
@Service
public class CallCostingService {

    private final EndpointRepository endpointRepository;
    private final CallRepository callRepository;

    public void applyCosting(Call call, String dcontext) {
        if (call.getCallStatus() != null) {
            return;
        }

        Circuit circuit = call.getCircuit();
        if (circuit == null) {
            markOutOfCost(call, CallStatus.NO_CIRCUIT);
            return;
        }

        boolean isOutbound = endpointRepository.findById(circuit.getNumber())
                .map(ep -> ep.getContext() != null && ep.getContext().equals(dcontext))
                .orElse(false);

        if (!isOutbound) {
            markOutOfCost(call, CallStatus.OUT_OF_SCOPE);
            return;
        }

        Plan plan = circuit.getPlan();
        if (plan == null) {
            markOutOfCost(call, CallStatus.NO_PLAN);
            return;
        }

        int billSeconds = call.getBillSeconds();
        if (billSeconds <= 3) {
            markOutOfCost(call, CallStatus.PROCESSED);
            return;
        }

        int callMonth = call.getCallDate().getMonthValue();
        int callYear  = call.getCallDate().getYear();

        int quota = resolveQuota(plan, call.getCallType(), circuit, callRepository);

        if (quota > 0) {
            applyWithQuota(call, plan, quota, billSeconds, callMonth, callYear);
        } else {
            call.setMinutesFromQuota(BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY));
            call.setCost(calculateFractionCost(billSeconds, resolveRate(plan, call.getCallType())));
        }

        call.setCallStatus(CallStatus.PROCESSED);
    }

    private void applyWithQuota(Call call, Plan plan, int quota, int billSeconds,
                                int callMonth, int callYear) {
        BigDecimal used      = fetchUsedMinutes(plan, call.getCallType(), call.getCircuit(), callRepository,
                callMonth, callYear);
        BigDecimal remaining = BigDecimal.valueOf(quota).subtract(used);
        BigDecimal durationMinutes = BigDecimal.valueOf(Math.ceil(billSeconds / 30.0))
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);

        if (remaining.compareTo(durationMinutes) >= 0) {
            call.setMinutesFromQuota(durationMinutes);
            call.setCost(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            int billableSeconds = billSeconds - remaining.multiply(BigDecimal.valueOf(60)).intValue();
            call.setMinutesFromQuota(remaining);
            call.setCost(calculateFractionCost(billableSeconds, resolveRate(plan, call.getCallType())));
        } else {
            call.setMinutesFromQuota(BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY));
            call.setCost(calculateFractionCost(billSeconds, resolveRate(plan, call.getCallType())));
        }
    }

    private static int resolveQuota(Plan plan, CallType callType, Circuit circuit, CallRepository repo) {
        return switch (plan.getPackageType()) {
            case UNIFIED      -> plan.getPackageTotalMinutes() != null ? plan.getPackageTotalMinutes() : 0;
            case PER_CATEGORY -> resolvePerCategoryQuota(plan, callType);
            case NONE         -> 0;
        };
    }

    private static BigDecimal fetchUsedMinutes(Plan plan, CallType callType, Circuit circuit,
                                               CallRepository repo, int month, int year) {
        return switch (plan.getPackageType()) {
            case UNIFIED      -> repo.sumQuotaMinutes(circuit.getNumber(), month, year);
            case PER_CATEGORY -> repo.sumQuotaMinutesByType(circuit.getNumber(), callType.name(), month, year);
            case NONE         -> BigDecimal.ZERO;
        };
    }

    private static int resolvePerCategoryQuota(Plan plan, CallType callType) {
        return switch (callType) {
            case FIXED_LOCAL          -> plan.getPackageFixedLocal()         != null ? plan.getPackageFixedLocal()         : 0;
            case FIXED_LONG_DISTANCE  -> plan.getPackageFixedLongDistance()  != null ? plan.getPackageFixedLongDistance()  : 0;
            case MOBILE_LOCAL         -> plan.getPackageMobileLocal()        != null ? plan.getPackageMobileLocal()        : 0;
            case MOBILE_LONG_DISTANCE -> plan.getPackageMobileLongDistance() != null ? plan.getPackageMobileLongDistance() : 0;
            default                   -> 0;
        };
    }

    private static BigDecimal resolveRate(Plan plan, CallType callType) {
        return switch (callType) {
            case FIXED_LOCAL          -> plan.getFixedLocal();
            case FIXED_LONG_DISTANCE  -> plan.getFixedLongDistance();
            case MOBILE_LOCAL         -> plan.getMobileLocal();
            case MOBILE_LONG_DISTANCE -> plan.getMobileLongDistance();
            default                   -> BigDecimal.ZERO;
        };
    }

    public static BigDecimal calculateFractionCost(int seconds, BigDecimal ratePerMinute) {
        if (seconds <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
        }
        int fractions = (int) Math.ceil(seconds / 30.0);
        return ratePerMinute
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(fractions))
                .setScale(3, RoundingMode.HALF_UP);
    }

    private static void markOutOfCost(Call call, CallStatus status) {
        call.setCallStatus(status);
        call.setCost(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
        call.setMinutesFromQuota(BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY));
    }
}
