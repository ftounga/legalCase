package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseNoteRequest(
        @NotBlank(message = "content is required")
        @Size(max = 5000, message = "content must not exceed 5000 characters")
        String content
) {}
