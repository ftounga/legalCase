package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-24-01 : réponse de l'endpoint d'analyse de validité d'une clause de
 * non-concurrence (FR).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les sorties calculées.</p>
 */
public record NonConcurrenceResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean clausePresenteContrat,
        Boolean limiteTerritoireDefini,
        String territoireDescription,
        Boolean limiteDureeDefinie,
        Integer dureeMois,
        Boolean limiteObjetDefini,
        String objetDescription,
        Boolean contrepartieFinancierePresente,
        BigDecimal contrepartieMontantMensuelEur,
        BigDecimal salaireMensuelBrutEur,
        NonConcurrenceCalculator.SecteurActivite secteurActivite,
        LocalDate datePriseEffet,
        // --- Outputs calculés ---
        boolean critere1TerritoireOk,
        boolean critere2DureeOk,
        boolean critere3ObjetOk,
        boolean critere4ContrepartieOk,
        BigDecimal ratioContrepartiePct,
        int scoreValidite,
        NonConcurrenceCalculator.VerdictValidite verdictValidite,
        BigDecimal indemniteContrepartieDueEur,
        BigDecimal indemnitePotentielleNulliteEur,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
