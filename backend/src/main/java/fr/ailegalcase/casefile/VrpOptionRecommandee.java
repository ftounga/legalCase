package fr.ailegalcase.casefile;

/**
 * SF-218-11 : option d'indemnisation recommandée pour le VRP au titre de la règle
 * de non-cumul. L'indemnité de clientèle (art. L.7313-13 CT) ne se cumule pas avec
 * l'indemnité légale de licenciement (art. R.1234-2 CT) : le VRP perçoit la plus
 * élevée. La recommandation compare {@code indemniteClienteleMax} à
 * {@code indemniteLegaleLicenciement} et reste à confirmer par l'avocat. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public enum VrpOptionRecommandee {
    INDEMNITE_CLIENTELE,
    INDEMNITE_LEGALE
}
