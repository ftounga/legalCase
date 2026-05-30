package fr.ailegalcase.casefile;

/**
 * SF-218-09 : type d'organisation exerçant l'action de groupe en discrimination
 * au travail (qualité à agir, art. L. 1134-7 Code travail). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>SYNDICAT_REPRESENTATIF : organisation syndicale représentative au sens
 *       de l'art. L. 1134-7 CT — habilitée à agir.</li>
 *   <li>ASSOCIATION_AGREEE_5ANS : association régulièrement déclarée depuis au
 *       moins 5 ans intervenant dans la lutte contre les discriminations —
 *       habilitée à agir (art. L. 1134-7 CT).</li>
 *   <li>AUTRE : toute autre structure — non habilitée à exercer l'action de
 *       groupe (verdict IRRECEVABLE_QUALITE).</li>
 * </ul>
 */
public enum ActionGroupeDiscriminationTypeOrganisation {
    SYNDICAT_REPRESENTATIF,
    ASSOCIATION_AGREEE_5ANS,
    AUTRE
}
