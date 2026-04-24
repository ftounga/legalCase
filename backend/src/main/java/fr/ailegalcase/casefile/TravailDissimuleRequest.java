package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-DT-21-01 : requête pour le calcul d'indemnité forfaitaire pour travail dissimulé.
 *
 * @param salaireMensuelReference salaire mensuel de référence retenu (>0)
 */
public record TravailDissimuleRequest(
        BigDecimal salaireMensuelReference
) {}
