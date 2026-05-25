package fr.ailegalcase.superadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * F-251 SF-251-03 — payload de l'endpoint super-admin
 * {@code POST /api/v1/super-admin/prospect-bootstrap}.
 *
 * <p>Remplace la chaîne d'{@code INSERT} SQL de la skill
 * {@code prospect-account-bootstrap} par un appel HTTP métier qui passe par
 * JPA, donc bénéficie automatiquement du garde-fou {@code @PrePersist}
 * SF-251-02 sur {@code Subscription.expiresAt}.
 *
 * <p>Validation Bean Validation : email format + length, password 8-100,
 * country et legalDomain restreints aux valeurs canoniques V1.
 */
public record ProspectBootstrapRequest(

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
        String lastName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 320, message = "L'email ne peut pas dépasser 320 caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
        String password,

        @NotBlank(message = "Le pays est obligatoire")
        @Pattern(regexp = "FRANCE|BELGIQUE",
                message = "Pays non reconnu — valeurs acceptées : FRANCE, BELGIQUE")
        String country,

        @NotBlank(message = "Le domaine juridique est obligatoire")
        @Pattern(regexp = "DROIT_DU_TRAVAIL|DROIT_IMMIGRATION|DROIT_FAMILLE",
                message = "Domaine juridique non reconnu — valeurs acceptées : "
                        + "DROIT_DU_TRAVAIL, DROIT_IMMIGRATION, DROIT_FAMILLE")
        @Size(max = 50)
        String legalDomain,

        @NotBlank(message = "Le nom du workspace est obligatoire")
        @Size(min = 1, max = 100, message = "Le nom du workspace doit contenir entre 1 et 100 caractères")
        String workspaceName
) {}
