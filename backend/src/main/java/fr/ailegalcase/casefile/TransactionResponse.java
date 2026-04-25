package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-31-01 : réponse de l'endpoint d'analyse de validité d'un protocole
 * transactionnel (FR).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées.</p>
 */
public record TransactionResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        LocalDate dateSignature,
        List<TransactionCalculator.ConcessionEmployeur> concessionsEmployeur,
        List<TransactionCalculator.ConcessionSalarie> concessionsSalarie,
        BigDecimal indemniteTransactionnelleEur,
        BigDecimal salaireMensuelBrutEur,
        Integer ancienneteAnnees,
        Boolean renonciationActionExpresse,
        Boolean delaiReflexion15jOk,
        TransactionCalculator.RupturePrealable rupturePrealable,
        Boolean presenceAvocatAssistance,
        Boolean viceConsentementAllegue,
        // --- Outputs calculés ---
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
