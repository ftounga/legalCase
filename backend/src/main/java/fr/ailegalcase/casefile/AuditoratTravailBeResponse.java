package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-25 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/auditorat-travail-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link CodePenalSocialBeResponse} SF-219-24).</p>
 */
public record AuditoratTravailBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        AuditoratTravailBeNatureFait natureFait,
        AuditoratTravailBeModeSaisine modeSaisineEnvisage,
        LocalDate dateFaits,
        boolean faitsPrescrits,
        boolean inspectionDejaSaisie,
        boolean plainteCivileDeposee,
        boolean urgenceSecuritePersonnes,
        boolean recoursPenalEnvisage,
        boolean employeurPersonneMorale,

        // Outputs
        AuditoratTravailBeVerdict verdict,
        AuditoratTravailBeModeSaisine modeSaisineRecommande,
        boolean constitutionPartieCivileRecommandee,
        boolean inspectionPrealableRequise,
        boolean voieCivileConcurrente,
        boolean prescriptionBloquante,
        boolean urgenceSignalee,
        boolean unaViaApplicable,
        boolean avisAuditeurObligatoireCivil,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
