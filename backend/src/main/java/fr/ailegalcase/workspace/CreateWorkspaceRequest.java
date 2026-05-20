package fr.ailegalcase.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Requête de création d'un workspace.
 *
 * <p>SF-156-01 : ajout du champ {@code plan} obligatoire (TEAM ou PRO).
 * Pour un workspace supplémentaire, l'OWNER courant doit lui-même être en
 * TEAM ou PRO actif — gate appliqué dans {@code WorkspaceService}.
 *
 * <p>Note : {@code legalDomain} et {@code country} restent obligatoires
 * pour préserver la configuration métier minimale du workspace. Cette
 * extension du contrat (par rapport au champ {@code plan} mentionné dans
 * la mini-spec) est documentée dans la PR.
 */
public record CreateWorkspaceRequest(
        @NotBlank(message = "Le nom du workspace est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String name,

        @NotBlank(message = "Le domaine juridique est obligatoire")
        @Pattern(regexp = "DROIT_DU_TRAVAIL|DROIT_IMMIGRATION|DROIT_FAMILLE",
                message = "Domaine juridique non reconnu")
        @Size(max = 50)
        String legalDomain,

        @NotBlank(message = "Le pays est obligatoire")
        @Pattern(regexp = "FRANCE|BELGIQUE",
                message = "Pays non reconnu — valeurs acceptées : FRANCE, BELGIQUE")
        String country,

        /**
         * SF-156-01 : plan du nouveau workspace (workspace supplémentaire).
         *
         * <p><strong>Sémantique conditionnelle</strong> :
         * <ul>
         *   <li>Premier workspace (onboarding, aucun membership existant) :
         *       champ <strong>optionnel</strong> et <strong>ignoré</strong> —
         *       le workspace est créé en FREE / ACTIVE (rétrocompatibilité
         *       F-08).</li>
         *   <li>Workspace supplémentaire : champ <strong>obligatoire</strong>,
         *       et seules les valeurs {@code TEAM} et {@code PRO} sont
         *       acceptées. La validation null / valeur invalide est faite
         *       côté service (renvoie 400) car @NotBlank casserait le cas
         *       premier workspace.</li>
         * </ul>
         *
         * <p>La validation {@code @Pattern} permet — si le champ est fourni —
         * de garantir uniquement TEAM ou PRO. Le rejet null pour un workspace
         * supplémentaire est appliqué dans
         * {@link WorkspaceService#createWorkspace} (400).
         */
        @Pattern(regexp = "TEAM|PRO",
                message = "Plan invalide — seuls TEAM et PRO sont acceptés pour un workspace supplémentaire")
        String plan
) {}
