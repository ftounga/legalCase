package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-31 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/conge-paternite-naissance-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219.</p>
 */
public record CongePaterniteNaissanceBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        CongePaterniteNaissanceBeStatutTravailleur statutTravailleur,
        CongePaterniteNaissanceBeLienFiliation lienFiliation,
        CongePaterniteNaissanceBeEtape etapeProcedure,
        boolean filiationEtablie,
        boolean contratTravailEnCours,
        boolean notificationEmployeurFaite,
        LocalDate dateNaissance,
        LocalDate dateNotificationEmployeur,
        Integer joursDejaPrisOuvrables,
        LocalDate dateFinPriseEffective,

        // Outputs analytiques
        CongePaterniteNaissanceBeVerdict verdict,
        int dureeApplicableJoursOuvrables,
        int joursRestantsAPrendre,
        LocalDate echeanceQuatreMoisPostNaissance,
        LocalDate finProtectionLicenciement,
        boolean protectionLicenciementActive,
        int indemniteEmployeurJoursCent,
        int indemniteMutuelleJoursQuatreVingtDeux,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
