package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-09 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/elections-sociales-be}.
 *
 * <p>Snapshot complet (inputs + outputs : verdict, obligation
 * CE / CPPT, calendrier complet, fenêtre protégée candidats) —
 * pattern uniforme avec les autres outils décisionnels BE de la
 * F-219 (miroir {@link LicenciementBeCollectifRenaultResponse}
 * SF-219-07).</p>
 */
public record ElectionsSocialesBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        ElectionsSocialesBeCycle cycleElectoral,
        LocalDate dateJourY,
        Integer effectifMoyenEtp,
        Boolean uniteTechniqueExploitationConfirmee,

        // Verdict
        ElectionsSocialesBeVerdict verdict,
        boolean conseilEntrepriseObligatoire,
        boolean comitePreventionProtectionObligatoire,
        int seuilCeApplique,
        int seuilCpptApplique,

        // Calendrier (calculé à partir de Y)
        LocalDate jourX,
        LocalDate dateDebutProcedureUte,
        LocalDate dateTransmissionListesElecteurs,
        LocalDate dateDepotListesCandidats,
        LocalDate dateAffichageCandidats,
        LocalDate dateProclamationResultats,
        LocalDate datePremiereReunionInstance,

        // Fenêtre de protection candidats
        LocalDate dateDebutPeriodeProtegeeCandidats,
        LocalDate dateFinPeriodeProtegeeNonElus,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
