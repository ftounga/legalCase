package fr.ailegalcase.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "L'email est obligatoire")
        String email
) {}
