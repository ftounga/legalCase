package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-14 : requête HTTP pour les endpoints d'analyse de protection du majeur BE.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.
 * Tous les champs enum sont obligatoires ; les boolean ont leur valeur par
 * défaut côté contrat ({@code false}). {@code mandatExtraJudiciaireDateSignature}
 * devient obligatoire si {@code mandatExtraJudiciaireSigne = true}.
 * {@code commentaire} est optionnel (max 1000 caractères).</p>
 */
public record ProtectionMajeurBeRequest(
        ProtectionMajeurBeCalculator.NatureAlterationBe natureAlteration,
        ProtectionMajeurBeCalculator.GraviteIncapaciteBe graviteIncapacite,
        Boolean mandatExtraJudiciaireSigne,
        LocalDate mandatExtraJudiciaireDateSignature,
        Boolean declarationAnticipeeExiste,
        Boolean environnementFamilialProtecteur,
        ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe niveauUrgence,
        ProtectionMajeurBeCalculator.ModeSaisineProtectionBe modeSaisineEnvisage,
        String commentaire
) {

    ProtectionMajeurBeInput toInput() {
        return new ProtectionMajeurBeInput(
                natureAlteration,
                graviteIncapacite,
                Boolean.TRUE.equals(mandatExtraJudiciaireSigne),
                mandatExtraJudiciaireDateSignature,
                Boolean.TRUE.equals(declarationAnticipeeExiste),
                Boolean.TRUE.equals(environnementFamilialProtecteur),
                niveauUrgence,
                modeSaisineEnvisage,
                commentaire
        );
    }
}
