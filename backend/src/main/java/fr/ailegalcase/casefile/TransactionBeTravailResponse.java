package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-213-06 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 (miroir
 * {@link LicenciementBeProtectionGrossesseResponse} SF-213-05).</p>
 */
public record TransactionBeTravailResponse(
        UUID caseFileId,

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
