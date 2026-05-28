package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-32 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219.</p>
 */
public record InterruptionCarriereSoinsParentalResponse(
        UUID caseFileId,

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

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
