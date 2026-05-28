package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-09 : résultat brut produit par {@link
 * ElectionsSocialesBeCalculator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link ElectionsSocialesBeService} dans
 * {@link ElectionsSocialesBeResponse}).</p>
 *
 * <p>Le calendrier complet (jour X, jour X-60, jour X-35, jour X+35,
 * jour X+40, jour Y, jour Y+6, jour Y+45) est <b>toujours calculé</b>,
 * indépendamment du verdict — même si l'entreprise n'est pas obligée
 * de tenir des élections, l'avocat peut vouloir afficher le
 * calendrier théorique pour conseil prévisionnel.</p>
 *
 * <p>La fenêtre de protection des candidats (Loi 19/03/1991) n'est
 * remplie que si une obligation existe — sinon {@code null}.</p>
 */
public record ElectionsSocialesBeResult(
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
        LocalDate jourX,                              // Y - 90 : affichage avis
        LocalDate dateDebutProcedureUte,              // X - 60
        LocalDate dateTransmissionListesElecteurs,    // X - 35
        LocalDate dateDepotListesCandidats,           // X + 35
        LocalDate dateAffichageCandidats,             // X + 40 — début période protégée
        LocalDate dateProclamationResultats,          // Y + 6
        LocalDate datePremiereReunionInstance,        // Y + 45

        // Fenêtre de protection candidats (Loi 19/03/1991)
        LocalDate dateDebutPeriodeProtegeeCandidats,  // jour X + 10 ≈ 30j avant affichage
        LocalDate dateFinPeriodeProtegeeNonElus,      // affichage + 2 ans

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
