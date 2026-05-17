package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-52 — cycle de vie d'une version de conclusions, indépendant du
 * {@link CaseConclusionStatus} (qui décrit l'état de génération IA).
 *
 * <p>Transitions libres entre les 3 états (l'avocat peut revenir de {@code VALIDATED}
 * à {@code DRAFT}). Seule contrainte métier : la génération doit être {@code DONE}
 * pour passer à {@code VALIDATED} ou {@code DEPOSITED}.</p>
 */
public enum ConclusionLifecycleStatus {

    /** Brouillon — état initial de toute version créée. */
    DRAFT,

    /** Validé par l'avocat — la génération doit être {@code DONE}. */
    VALIDATED,

    /** Déposé auprès de la juridiction — la génération doit être {@code DONE}. */
    DEPOSITED
}
