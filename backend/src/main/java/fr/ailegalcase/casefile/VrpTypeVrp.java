package fr.ailegalcase.casefile;

/**
 * SF-218-11 : type de VRP statutaire (art. L.7311-1 et s. CT).
 *
 * <p>Le VRP <b>exclusif</b> travaille pour un seul employeur ; le VRP
 * <b>multicartes</b> représente plusieurs employeurs. Le régime de l'indemnité
 * de clientèle et du préavis (art. L.7313-9 / L.7313-13 CT) est identique ; la
 * répartition de l'indemnité entre employeurs pour le multicartes est hors
 * périmètre V1. Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum VrpTypeVrp {
    EXCLUSIF,
    MULTICARTES
}
