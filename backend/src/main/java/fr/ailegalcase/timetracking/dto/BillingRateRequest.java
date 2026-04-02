package fr.ailegalcase.timetracking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BillingRateRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "ratePerHour must be greater than 0")
        @DecimalMax(value = "9999.99", message = "ratePerHour must not exceed 9999.99")
        BigDecimal ratePerHour
) {}
