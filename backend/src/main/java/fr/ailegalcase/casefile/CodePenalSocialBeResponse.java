package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-24 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/code-penal-social-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link EgaliteFemmesHommesBeResponse} SF-219-22).</p>
 */
public record CodePenalSocialBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        CodePenalSocialBeTypeInfraction typeInfraction,
        Integer niveauPropose,
        LocalDate dateFaits,
        int nombreTravailleursConcernes,
        boolean personneMorale,
        boolean recidiveDansLAn,
        boolean prevenuPreposeOuMandataire,
        boolean elementMoralIntentionnel,

        // Outputs
        CodePenalSocialBeVerdict verdict,
        int niveauSanction,
        int amendeAdminMin,
        int amendeAdminMax,
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
