package fr.ailegalcase.casefile;

/**
 * SF-218-25 : fondement juridique du recours au CDI de chantier / d'opération
 * (art. L.1223-8 et L.1223-9 CT, F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ACCORD_BRANCHE_ETENDU : le recours repose sur un accord de branche
 *       étendu (art. L.1223-8) — fondement le plus solide.</li>
 *   <li>USAGE_CONSTANT_SECTEUR : à défaut d'accord, recours admis par un usage
 *       constant dans certains secteurs (BTP, ingénierie — art. L.1223-9).</li>
 *   <li>AUCUN : aucun fondement (ni accord étendu, ni usage constant) → risque
 *       de requalification en CDI de droit commun.</li>
 * </ul>
 */
public enum CdiChantierFondementRecours {
    ACCORD_BRANCHE_ETENDU,
    USAGE_CONSTANT_SECTEUR,
    AUCUN
}
