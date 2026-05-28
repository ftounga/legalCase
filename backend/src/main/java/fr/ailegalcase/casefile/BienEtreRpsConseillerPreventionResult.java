package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-30 : résultat brut produit par
 * {@link BienEtreRpsConseillerPreventionChecker} — fonction <b>pure</b>
 * (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link BienEtreRpsConseillerPreventionService} dans
 * {@link BienEtreRpsConseillerPreventionResponse}).</p>
 *
 * <p>Outputs dérivés : verdict de conformité procédurale, drapeaux
 * d'avancement (entretien préalable, écrit signé, AR, notification
 * employeur), dates calculées (échéance 3 mois enquête, échéance
 * 2 mois mesures employeur, fin protection 12 mois représailles).</p>
 */
public record BienEtreRpsConseillerPreventionResult(
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

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
