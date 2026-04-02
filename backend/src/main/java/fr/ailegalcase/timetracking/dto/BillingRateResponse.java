package fr.ailegalcase.timetracking.dto;

import fr.ailegalcase.timetracking.entity.UserBillingRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BillingRateResponse(
        UUID id,
        BigDecimal ratePerHour,
        LocalDate effectiveFrom
) {
    public static BillingRateResponse from(UserBillingRate rate) {
        return new BillingRateResponse(rate.getId(), rate.getRatePerHour(), rate.getEffectiveFrom());
    }
}
