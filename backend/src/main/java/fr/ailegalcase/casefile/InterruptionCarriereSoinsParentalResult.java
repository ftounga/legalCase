package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-32 : résultat brut produit par
 * {@link InterruptionCarriereSoinsParentalChecker} — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link InterruptionCarriereSoinsParentalService} dans
 * {@link InterruptionCarriereSoinsParentalResponse}).</p>
 *
 * <p>Outputs dérivés : verdict d'éligibilité, durée effective de
 * l'interruption en mois (4 / 8 / 20), allocations ONEM mensuelle et
 * totale forfaitaires, solde restant après imputation, dates calculées
 * (fin interruption, fin protection 3 mois après), indemnité protection
 * 6 mois rémunération brute.</p>
 */
public record InterruptionCarriereSoinsParentalResult(
        // Inputs (snapshot)
        InterruptionCarriereSoinsParentalForme forme,
        int ancienneteMois,
        int ageEnfantMois,
        boolean enfantHandicap,
        int soldeRestantMoisEtp,
        InterruptionCarriereSoinsParentalModeNotification modeNotification,
        boolean employeurAccepte,
        boolean employeurAdiffere,
        boolean cumulAllocationOnemDemande,
        LocalDate dateDebutInterruption,
        LocalDate dateNotification,
        BigDecimal remunerationMensuelleBrute,

        // Outputs analytiques
        InterruptionCarriereSoinsParentalVerdict verdict,
        boolean ancienneteOk,
        boolean ageEnfantOk,
        boolean formalismeOk,
        int dureeInterruptionMois,
        int soldeRestantApresImputationMoisEtp,
        BigDecimal allocationOnemMensuelle,
        BigDecimal allocationOnemTotale,
        LocalDate dateFinInterruption,
        LocalDate finProtectionLicenciement,
        boolean protectionLicenciementActive,
        BigDecimal indemniteProtectionEnCasLicenciement,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
