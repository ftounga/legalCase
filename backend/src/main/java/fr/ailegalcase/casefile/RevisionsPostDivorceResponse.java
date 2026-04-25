package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-13-01 : réponse exposée au frontend (SF-FA-13-02).
 */
public record RevisionsPostDivorceResponse(
        UUID caseFileId,
        String typeRevision,
        LocalDate dateDecisionInitiale,
        String changementCirconstance,
        BigDecimal revenusInitialsDebiteurEur,
        BigDecimal revenusActuelsDebiteurEur,
        BigDecimal revenusInitialsCreancierEur,
        BigDecimal revenusActuelsCreancierEur,
        Integer nbEnfantsACharge,
        List<Integer> ageEnfants,
        String modeResidenceActuel,
        String modeResidenceDemande,
        Boolean informationPrealable,
        Integer distanceDemenagementKm,
        String country,
        Integer ecartRevenusPct,
        int ancienneteDecisionMois,
        boolean modificationSubstantielle,
        boolean motivationSuffisante,
        int scoreGlobal,
        String verdictRevisionPossible,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
