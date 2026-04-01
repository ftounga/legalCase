package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CaseDeadlineValidateRequest(
        @NotNull @Pattern(regexp = "ACCEPT|REJECT") String action
) {}
