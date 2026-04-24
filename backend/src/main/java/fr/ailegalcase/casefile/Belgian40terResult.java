package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-14-04 : résultat de l'analyse 40ter familial Belge BE (Loi 15/12/1980
 * art. 40ter). Outil single-country BE — régime strict réservé aux regroupants
 * de nationalité belge.
 */
public record Belgian40terResult(
        String lienFamilial,
        boolean regroupantBelge,
        BigDecimal revenusMensuelsNetsEur,
        BigDecimal seuil120PctRisEur,
        boolean assuranceMaladie,
        boolean logementSuffisant,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        boolean lienValide,
        boolean regroupantBelgeOk,
        boolean revenusSuffisantsOk,
        boolean assuranceOk,
        boolean logementOk,
        boolean pasMenace,
        BigDecimal differentielRevenus,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
