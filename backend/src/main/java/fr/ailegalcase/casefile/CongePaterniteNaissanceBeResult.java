package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-31 : résultat brut produit par
 * {@link CongePaterniteNaissanceBeChecker} — fonction <b>pure</b>
 * (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link CongePaterniteNaissanceBeService} dans
 * {@link CongePaterniteNaissanceBeResponse}).</p>
 *
 * <p>Outputs dérivés : durée applicable selon le régime temporel
 * (20 / 15 / 10 jours), jours restants à prendre, échéance des 4 mois
 * post-naissance, fin de protection licenciement 5 mois, drapeau
 * protection active, et verdict consolidé.</p>
 */
public record CongePaterniteNaissanceBeResult(
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

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
