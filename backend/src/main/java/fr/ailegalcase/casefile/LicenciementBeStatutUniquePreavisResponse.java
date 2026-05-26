package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-213-03 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis}.
 */
public record LicenciementBeStatutUniquePreavisResponse(
        UUID caseFileId,
        int ancienneteAnnees,
        int ancienneteMoisSupplementaires,
        BigDecimal salaireHebdomadaireBrut,
        LocalDate dateNotificationLicenciement,
        boolean partieStatutUniqueSeulement,
        int dureePreavisEnSemaines,
        LocalDate dateFinPreavis,
        BigDecimal indemniteCompensatoire,
        String formuleCalcul,
        String baseJuridique,
        String avertissement
) {}
