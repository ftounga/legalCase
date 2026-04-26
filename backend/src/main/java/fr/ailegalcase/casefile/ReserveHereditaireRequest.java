package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-FA-24-07 : requête d'analyse de réserve héréditaire et action en réduction
 * (FR — art. 913 + 914-1 + 920-928 Cciv). Le pays n'est pas transmis dans le
 * body — il est dérivé de {@code caseFile.getWorkspace().getCountry()} côté
 * service.
 */
public record ReserveHereditaireRequest(
        Integer nombreEnfants,
        Boolean conjointSurvivant,
        BigDecimal montantSuccession,
        BigDecimal montantLibsTotal,
        LocalDate dateOuvertureSuccession,
        ReserveHereditaireCalculator.QualiteDemandeur qualiteDuDemandeur
) {}
