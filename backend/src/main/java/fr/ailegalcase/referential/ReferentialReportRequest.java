package fr.ailegalcase.referential;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReferentialReportRequest(
        @NotBlank @Size(max = 500) String comment
) {}
