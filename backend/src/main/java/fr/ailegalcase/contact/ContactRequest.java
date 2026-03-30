package fr.ailegalcase.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(

        @NotBlank
        @Size(max = 100)
        String nom,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Pattern(regexp = "[\\d\\s\\+\\-\\(\\)]{7,20}", message = "Format de téléphone invalide")
        @Size(max = 20)
        String telephone,

        @NotBlank
        @Size(max = 200)
        String sujet,

        @NotBlank
        @Size(max = 3000)
        String message
) {}
