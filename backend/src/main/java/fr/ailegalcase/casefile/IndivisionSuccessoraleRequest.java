package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-FA-24-11 : requête d'analyse d'une indivision successorale (art. 815 à
 * 832-2 + 1873-1 et s. + 815-1 et s. Cciv). Outil single-country FR
 * DROIT_FAMILLE.
 *
 * <p>À distinguer de F-FA-22 (indivision post-communautaire, suite divorce) —
 * ici l'origine est une succession.</p>
 */
public record IndivisionSuccessoraleRequest(
        LocalDate dateOuvertureSuccession,
        String typeIndivision,
        Integer nbHeritiers,
        BigDecimal valeurPatrimoineIndivisEur,
        BigDecimal valeurBienOccupeEur,
        Boolean consentementsTous,
        Boolean occupationExclusive,
        Boolean actesAdministrationContestes,
        Boolean demandePartage
) {}
