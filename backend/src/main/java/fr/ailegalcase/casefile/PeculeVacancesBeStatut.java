package fr.ailegalcase.casefile;

/**
 * SF-219-20 : statut du travailleur pour le calcul du pécule de
 * vacances BE (Lois coordonnées du 28/06/1971 relatives aux vacances
 * annuelles des travailleurs salariés ; AR du 30/03/1967 déterminant
 * les modalités générales d'exécution).
 *
 * <p>Le régime distingue fondamentalement <b>employés</b> (pécule
 * payé directement par l'employeur sur la rémunération de l'exercice
 * en cours) et <b>ouvriers</b> (pécule payé par l'<i>Office National
 * des Vacances Annuelles</i> — ONVA — ou une <i>Caisse spéciale de
 * vacances</i> sectorielle, sur la base des cotisations versées par
 * l'employeur). Le statut « jeune travailleur » donne droit à un
 * pécule de vacances jeunes calculé sur le revenu d'intégration ou
 * l'allocation de chômage de l'année précédente (art. 9 et 9bis AR
 * 30/03/1967).</p>
 */
public enum PeculeVacancesBeStatut {
    /** Employé(e) — pécule payé par l'employeur. */
    EMPLOYE,

    /** Ouvrier(ère) — pécule payé par l'ONVA / Caisse de vacances. */
    OUVRIER,

    /** Jeune travailleur 1re année (régime art. 9 AR 30/03/1967). */
    JEUNE_TRAVAILLEUR,

    /** Statut indéterminé — analyse cas par cas. */
    INDETERMINE
}
