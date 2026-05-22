package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-03 : réponse API /api/v1/case-files/{id}/pension-alimentaire-enfant-fr.
 */
public record PensionAlimentaireEnfantFrResponse(
        UUID caseFileId,
        List<Integer> montantParEnfantMensuelEur,
        int totalMensuelEur,
        double tauxApplique,
        double coefficientResidence,
        String parentDebiteur,
        String baseJuridique,
        List<String> messages,
        List<String> alertes,
        String country
) {}
