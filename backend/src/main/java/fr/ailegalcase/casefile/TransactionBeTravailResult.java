package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-213-06 : résultat brut produit par
 * {@link TransactionBeTravailValidator} — fonction pure.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link TransactionBeTravailService} dans
 * {@link TransactionBeTravailResponse}).</p>
 *
 * <p>{@code raisonInvalidite} est {@code null} pour le verdict
 * {@link TransactionBeTravailVerdict#VALIDE}. {@code ratioPourcentage} et
 * {@code avertissement} sont {@code null} si
 * {@code indemniteLegaleEtimee} n'a pas été fournie ou est {@code <= 0}.</p>
 */
public record TransactionBeTravailResult(
        BigDecimal montantTransactionBrut,
        BigDecimal indemniteLegaleEtimee,
        boolean concessionsEmployeurDescrites,
        List<String> renonciationsListees,
        boolean renonciationOrdrePublicDetectee,
        boolean mentionContestation,

        TransactionBeTravailVerdict verdict,
        TransactionBeTravailRaisonInvalidite raisonInvalidite,
        BigDecimal ratioPourcentage,
        String avertissement,
        List<TransactionBeTravailChecklistItem> checklistRenonciations,
        String baseJuridique
) {
}
