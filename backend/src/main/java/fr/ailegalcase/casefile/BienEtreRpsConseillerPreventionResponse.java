package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-30 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/bien-etre-rps-conseiller-prevention}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219.</p>
 */
public record BienEtreRpsConseillerPreventionResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        BienEtreRpsConseillerPreventionTypeRisque typeRisque,
        BienEtreRpsConseillerPreventionModeDemande modeDemande,
        BienEtreRpsConseillerPreventionEtape etapeProcedure,
        boolean conseillerIdentifie,
        boolean entretienPrealableRealise,
        boolean demandeEcriteSignee,
        boolean accuseReceptionRecu,
        boolean notificationEmployeurFaite,
        LocalDate dateDepotDemande,
        LocalDate dateAvisRendu,
        Integer joursDepuisDepot,

        // Outputs analytiques
        BienEtreRpsConseillerPreventionVerdict verdict,
        boolean formalitesPrealablesCompletes,
        boolean delaiEnqueteRespecte,
        LocalDate echeanceEnqueteTroisMois,
        LocalDate echeanceMesuresEmployeurDeuxMois,
        LocalDate finProtectionRepresailles,
        boolean protectionRepresaillesActive,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
