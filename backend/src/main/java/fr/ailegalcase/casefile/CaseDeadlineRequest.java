package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CaseDeadlineRequest(
        @NotBlank @Size(max = 255) String label,
        @NotNull LocalDate dueDate
) {}
