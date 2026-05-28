package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-26 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/travail-noir-be-dimona}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link CodePenalSocialBeResponse} SF-219-24).</p>
 */
public record TravailNoirBeDimonaResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        TravailNoirBeDimonaStatutDimona statutDimona,
        LocalDate dateDebutOccupation,
        LocalDate dateDimonaEffective,
        LocalDate dateControle,
        BigDecimal salaireBrutMensuel,
        int nombreTravailleursConcernes,
        boolean personneMorale,
        boolean recidiveDansLAn,
        TravailNoirBeDimonaElementsSubordination elementsSubordination,

        // Outputs
        TravailNoirBeDimonaVerdict verdict,
        long dureeNonDeclareeJours,
        BigDecimal cotisationsOnssEmployeur,
        BigDecimal cotisationsOnssTravailleur,
        BigDecimal cotisationsOnssTotal,
        BigDecimal amendeOnssForfaitaire3x,
        boolean sanctionPenaleNiveau4Applicable,
        int amendePenaleAdminMin,
        int amendePenaleAdminMax,
        int amendePenaleMin,
        int amendePenaleMax,
        boolean emprisonnementApplicable,
        int emprisonnementMinMois,
        int emprisonnementMaxMois,
        boolean multiplicationParTravailleur,
        boolean majorationPersonneMorale,
        boolean majorationRecidive,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
