package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-DT-11-01 : requête pour le calcul d'indemnité minimum en cas de licenciement nul.
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 *
 * @param salaireMensuelReference salaire mensuel de référence retenu (>0)
 * @param motifNullite            motif de nullité (enum {@link HarcelementNulliteCalculator.MotifNullite})
 */
public record HarcelementNulliteRequest(
        BigDecimal salaireMensuelReference,
        HarcelementNulliteCalculator.MotifNullite motifNullite
) {}
