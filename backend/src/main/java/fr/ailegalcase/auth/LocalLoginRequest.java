package fr.ailegalcase.auth;

import jakarta.validation.constraints.NotBlank;

public record LocalLoginRequest(

        @NotBlank(message = "L'email est obligatoire")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {}
