package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseFileRequest(
        @NotBlank(message = "title is required") String title,
        @NotBlank(message = "legalDomain is required") String legalDomain,
        @Size(max = 2000, message = "description must not exceed 2000 characters") String description
) {
}
