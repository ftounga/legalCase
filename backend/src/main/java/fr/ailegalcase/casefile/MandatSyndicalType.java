package fr.ailegalcase.casefile;

/**
 * SF-218-33 : type de mandat syndical analysé (F-DT-69). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DELEGUE_SYNDICAL : délégué syndical (DS) désigné par une organisation
 *       syndicale représentative dans les entreprises d'au moins 50 salariés
 *       (art. L.2143-1 et s. CT). Le candidat doit en principe avoir recueilli
 *       au moins 10 % des suffrages au 1er tour des dernières élections
 *       (L.2143-3).</li>
 *   <li>RSS : représentant de section syndicale, désigné par un syndicat
 *       <b>non</b> représentatif (art. L.2142-1-1 CT) — pas de condition de
 *       score personnel.</li>
 * </ul>
 */
public enum MandatSyndicalType {
    DELEGUE_SYNDICAL,
    RSS
}
