package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-32-01 : résultat structuré du contrôle de conformité des documents de fin de contrat (FR).
 */
public record DocumentsFinContratResult(
        boolean certificatRemisDansDelai,
        boolean attestationRemiseDansDelai,
        boolean souldeToutCompteValide,
        boolean souldeToutCompteContestable,
        int totalSanctionsCumulables,
        BigDecimal indemniteRetardCertificatEur,
        BigDecimal indemniteRetardAttestationEur,
        int scoreConformiteEmployeur,
        DocumentsFinContratCalculator.VerdictRisqueContentieux verdictRisqueContentieux,
        String baseJuridique,
        String formule,
        List<String> messages,
        List<String> messagesContentieux,
        String country
) {}
