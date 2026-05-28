package fr.ailegalcase.casefile;

/**
 * SF-219-11 : verdict de l'outil <i>congé-éducation payé régionalisé</i>
 * (Loi 22/01/1985 régionalisée 2014 — WBR / FLA / BXL).
 *
 * <p>L'outil restitue l'éligibilité d'un travailleur au congé-éducation
 * payé en fonction de la région de son lieu de travail, du type de
 * formation suivie et de son temps de travail. Les trois régions
 * appliquent des plafonds et conditions distincts ; cet outil unique
 * branche en interne sur le régime applicable.</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_PLEIN_DROIT} — travailleur à temps plein
 *       suivant une formation agréée — accès au plafond régional
 *       intégral.</li>
 *   <li>{@link #ELIGIBLE_PRORATA} — travailleur à temps partiel (≥
 *       seuil régional, généralement mi-temps) — plafond proratisé
 *       au temps de travail.</li>
 *   <li>{@link #INELIGIBLE_HORS_FORMATION_AGREEE} — formation NON
 *       inscrite à la liste agréée régionale — aucun droit au congé.</li>
 *   <li>{@link #INELIGIBLE_OCCUPATION_INSUFFISANTE} — temps de travail
 *       sous le seuil régional (sauf dérogation publics fragilisés
 *       FLA) — aucun droit au congé.</li>
 *   <li>{@link #A_ANALYSER} — situation ambiguë (changement de région
 *       de lieu de travail, formation transfrontalière, statut
 *       hybride) — analyse juridique requise.</li>
 * </ul>
 */
public enum CongeEducationPayeRegionVerdict {
    /** Plein droit au plafond régional applicable. */
    ELIGIBLE_PLEIN_DROIT,

    /** Droit au prorata du temps de travail. */
    ELIGIBLE_PRORATA,

    /** Formation hors liste agréée régionale — aucun droit. */
    INELIGIBLE_HORS_FORMATION_AGREEE,

    /** Occupation sous seuil régional — aucun droit (sauf dérogation FLA). */
    INELIGIBLE_OCCUPATION_INSUFFISANTE,

    /** Situation ambiguë — avocat BE requis. */
    A_ANALYSER
}
