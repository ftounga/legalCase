package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-DT-28-01 : requête pour le calcul cumulé des avantages conventionnels belges.
 */
public record AvantagesConventionnelsBeRequest(
        BigDecimal salaireMensuelBrutEur,
        Integer joursTravaillesAnneePrecedente,
        Integer anciennetteMois,
        String commissionParitaire,
        Integer annee,
        Boolean doublePeculeVacancesPercu,
        Boolean primeFinAnneePrevueCcCp,
        Boolean ecoChequesPrevuCcCp,
        Boolean ecoChequesUtilisationDansAn,
        Boolean chequesRepasPrevu,
        Integer joursPrestesEffectifs
) {}
