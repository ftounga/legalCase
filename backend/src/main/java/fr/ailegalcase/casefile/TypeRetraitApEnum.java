package fr.ailegalcase.casefile;

/**
 * SF-216-11 : type de retrait d'autorité parentale envisagé par l'avocat
 * lorsqu'il interroge l'outil F-FA-RETRAIT-AP (art. 378-381 Cciv).
 *
 * <ul>
 *   <li>{@link #TOTAL} — retrait de l'intégralité de l'autorité parentale
 *       (art. 378 Cciv).</li>
 *   <li>{@link #PARTIEL_EXERCICE} — retrait partiel ne portant que sur
 *       l'exercice de l'autorité parentale (art. 378-1 al. 1 Cciv).</li>
 *   <li>{@link #PARTIEL_ATTRIBUTS} — retrait partiel ne portant que sur
 *       certains attributs (consentement adoption, autorisation, etc. —
 *       art. 378-1 al. 1 Cciv).</li>
 * </ul>
 */
public enum TypeRetraitApEnum {
    TOTAL,
    PARTIEL_EXERCICE,
    PARTIEL_ATTRIBUTS
}
