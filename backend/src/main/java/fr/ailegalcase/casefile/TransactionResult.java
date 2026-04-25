package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-31-01 : résultat structuré de l'analyse de validité d'un protocole
 * transactionnel (FR — art. 2044 à 2052 Cciv + Cass. soc. 16/12/2010).
 */
public record TransactionResult(
        boolean concessionsReciproquesCaracterisees,
        BigDecimal ratioConcessionsEmployeurPct,
        boolean indemniteTransactionnelleSuperieureMacron,
        int scoreValidite,
        TransactionCalculator.VerdictValidite verdictValiditeContrat,
        boolean risqueNulliteRetenu,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
