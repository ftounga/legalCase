package fr.ailegalcase.share;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateShareRequest(
        @Min(1) @Max(30) int expiresInDays
) {}
