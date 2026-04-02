package fr.ailegalcase.help;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HelpChatRequest(
        @NotBlank(message = "Le message ne peut pas être vide")
        @Size(max = 500, message = "Le message ne peut pas dépasser 500 caractères")
        String message
) {}
