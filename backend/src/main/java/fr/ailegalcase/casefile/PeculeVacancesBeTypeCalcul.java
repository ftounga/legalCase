package fr.ailegalcase.casefile;

/**
 * SF-219-20 : type de calcul demandé pour le pécule de vacances BE
 * (Lois coordonnées 28/06/1971 + AR 30/03/1967).
 *
 * <p>Trois calculs peuvent être demandés isolément ou ensemble selon
 * la situation :</p>
 * <ul>
 *   <li>{@link #PECULE_SIMPLE} — pécule simple : rémunération
 *       normale maintenue pendant les jours de congé (art. 38 AR
 *       30/03/1967 — employés), ou 7,67 % à 8 % de la rémunération
 *       brute annuelle 108 % de l'exercice de vacances pour les
 *       ouvriers via ONVA (art. 19 AR 30/03/1967).</li>
 *   <li>{@link #DOUBLE_PECULE} — supplément payé au moment des
 *       vacances principales (art. 38 AR 30/03/1967), 85 % du
 *       salaire mensuel brut pour les employés (et 7,67 % à 6,8 %
 *       pour les ouvriers via ONVA, calculé sur la rémunération brute
 *       annuelle 108 %).</li>
 *   <li>{@link #PECULE_DEPART} — pécule de sortie (« pécule de
 *       départ ») dû par l'employeur au moment du départ de
 *       l'employé(e) : 15,34 % de la rémunération brute de l'exercice
 *       de vacances en cours + 15,34 % de la rémunération brute de
 *       l'exercice précédent non encore liquidée (art. 46 AR
 *       30/03/1967). Pour les ouvriers, le pécule de départ est
 *       intégré dans le pécule ONVA de l'année suivante.</li>
 * </ul>
 */
public enum PeculeVacancesBeTypeCalcul {
    /** Pécule simple — rémunération normale des jours de congé. */
    PECULE_SIMPLE,

    /** Double pécule — supplément payé au moment des vacances. */
    DOUBLE_PECULE,

    /** Pécule de départ — payé en sortie (employés uniquement). */
    PECULE_DEPART
}
