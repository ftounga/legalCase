package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-13-01 : résultat de l'analyse de révision post-divorce FR
 * (art. 209 / 373-2-13 / 373-2-9 / 279 / 373-2 Cciv).
 *
 * <p>Outil <b>single-country FR</b>. Le mécanisme BE (art. 301 § 7 CC pour la
 * pension après divorce, art. 374 CC pour l'autorité parentale) repose sur des
 * bases juridiques distinctes et n'est pas couvert par cette SF.</p>
 */
public record RevisionsPostDivorceResult(
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
