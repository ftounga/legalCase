package fr.ailegalcase.casefile;

/**
 * SF-219-20 : verdict de l'outil <i>pécule de vacances BE</i> (Lois
 * coordonnées du 28/06/1971 + AR du 30/03/1967).
 *
 * <h2>Hiérarchie</h2>
 * <ul>
 *   <li>{@link #DU_PECULE_SIMPLE_CALCULE} — pécule simple liquidé,
 *       le calcul est cohérent et la créance reste due.</li>
 *   <li>{@link #DU_DOUBLE_PECULE_CALCULE} — double pécule liquidé,
 *       la créance reste due (vacances principales).</li>
 *   <li>{@link #DU_PECULE_DEPART_CALCULE} — pécule de départ liquidé
 *       à la sortie de l'employé(e), créance due par l'employeur.</li>
 *   <li>{@link #NON_DU_OUVRIER_VIA_ONVA} — l'ouvrier doit s'adresser
 *       à l'ONVA / Caisse de vacances et non à l'employeur (art.
 *       19 AR 30/03/1967 — pas d'action directe contre l'employeur
 *       hors hypothèse de défaut de versement de cotisations).</li>
 *   <li>{@link #NON_DU_DEJA_PAYE} — le pécule réclamé a déjà été
 *       liquidé (action irrecevable / sans objet).</li>
 *   <li>{@link #NON_DU_JOURS_INSUFFISANTS} — durée d'occupation
 *       insuffisante pour ouvrir le droit (vacances jeunes — art.
 *       9 AR 30/03/1967).</li>
 *   <li>{@link #PRESCRIT} — créance prescrite (art. 15 Loi 03/07/1978
 *       sur les contrats de travail : 1 an depuis la fin du contrat,
 *       5 ans depuis le fait générateur).</li>
 *   <li>{@link #A_ANALYSER} — configuration ambigüe.</li>
 * </ul>
 */
public enum PeculeVacancesBeVerdict {
    /** Pécule simple liquidé — créance due. */
    DU_PECULE_SIMPLE_CALCULE,

    /** Double pécule liquidé — créance due. */
    DU_DOUBLE_PECULE_CALCULE,

    /** Pécule de départ liquidé — créance due à la sortie. */
    DU_PECULE_DEPART_CALCULE,

    /** Ouvrier — action à diriger contre l'ONVA / Caisse de vacances. */
    NON_DU_OUVRIER_VIA_ONVA,

    /** Pécule déjà payé — action sans objet. */
    NON_DU_DEJA_PAYE,

    /** Durée d'occupation insuffisante — droit non ouvert. */
    NON_DU_JOURS_INSUFFISANTS,

    /** Créance prescrite (art. 15 Loi 03/07/1978). */
    PRESCRIT,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
