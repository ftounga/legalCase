package fr.ailegalcase.casefile;

/**
 * SF-218-09 : verdict de recevabilité de l'action de groupe en discrimination
 * au travail (art. L. 1134-7 à L. 1134-10 Code travail). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>RECEVABLE : qualité à agir établie (syndicat représentatif ou
 *       association agréée depuis ≥ 5 ans), pluralité de situations similaires
 *       et délai de carence de 6 mois après mise en demeure écoulé.</li>
 *   <li>PREMATURE : qualité et pluralité établies mais délai de carence de
 *       6 mois après mise en demeure non encore écoulé (art. L. 1134-9 CT) — la
 *       saisine est prématurée.</li>
 *   <li>IRRECEVABLE_QUALITE : organisation non habilitée à exercer l'action de
 *       groupe (art. L. 1134-7 CT).</li>
 *   <li>INFO_MANQUANTE : mise en demeure préalable de l'employeur absente —
 *       impossible d'apprécier le délai de carence ; item bloquant.</li>
 * </ul>
 */
public enum ActionGroupeDiscriminationVerdict {
    RECEVABLE,
    PREMATURE,
    IRRECEVABLE_QUALITE,
    INFO_MANQUANTE
}
