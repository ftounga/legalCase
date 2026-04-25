package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-28-01 : réponse de l'analyse des avantages conventionnels belges.
 */
public record AvantagesConventionnelsBeResponse(
        UUID caseFileId,
        BigDecimal salaireMensuelBrutEur,
        int joursTravaillesAnneePrecedente,
        int anciennetteMois,
        String commissionParitaire,
        int annee,
        boolean doublePeculeVacancesPercu,
        boolean primeFinAnneePrevueCcCp,
        boolean ecoChequesPrevuCcCp,
        boolean ecoChequesUtilisationDansAn,
        boolean chequesRepasPrevu,
        int joursPrestesEffectifs,
        BigDecimal peculeVacancesSimpleEur,
        BigDecimal doublePeculeVacancesEur,
        BigDecimal primeFinAnneeEur,
        BigDecimal ecoChequesValeurAnnuelleEur,
        BigDecimal chequesRepasValeurAnnuelleEur,
        BigDecimal totalAvantagesAnnuelsEur,
        String formule,
        String baseJuridique,
        List<String> messages,
        String country
) {}
