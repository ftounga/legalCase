package fr.ailegalcase.workspace;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WorkspaceInvitationRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "ADMIN|LAWYER|MEMBER", message = "Role must be ADMIN, LAWYER or MEMBER") String role
) {}
