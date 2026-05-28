package fr.ailegalcase.casefile;

/**
 * SF-219-18 : verdict de l'outil <i>semaine de 4 jours BE</i> (Loi
 * du 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022, art. 5).
 *
 * <h2>Conditions cumulatives pour valider la mise en place</h2>
 * <ol>
 *   <li><b>Demande écrite du travailleur</b> — art. 5 § 2 Loi
 *       03/10/2022 : la mise en place se fait <i>à la demande</i>
 *       du travailleur, par écrit (lettre recommandée ou remise en
 *       main propre contre accusé de réception).</li>
 *   <li><b>Travailleur à temps plein</b> — art. 5 § 1 : le régime
 *       n'est ouvert qu'au travailleur exerçant à temps plein
 *       (exclusion des temps partiels).</li>
 *   <li><b>Durée hebdomadaire conservée</b> — la durée hebdomadaire
 *       de travail (38 h à 40 h selon la CCT sectorielle) est
 *       maintenue, simplement compressée sur 4 jours.</li>
 *   <li><b>Journée maximale ≤ 9 h 30</b> — art. 5 § 3 : la journée
 *       de travail ne peut excéder 9 h 30 (dépassement de la limite
 *       générale de 9 h fixée par la Loi du 16/03/1971 sur le
 *       travail). Une CCT sectorielle peut porter à 10 h.</li>
 *   <li><b>Accord employeur formalisé par avenant</b> — art. 5 § 4 :
 *       l'accord prend la forme d'une annexe écrite au contrat de
 *       travail, d'une durée maximale de <b>6 mois renouvelables</b>.</li>
 *   <li><b>Modification corrélative du règlement de travail</b> —
 *       l'horaire compressé doit être inscrit au règlement de
 *       travail (procédure simplifiée art. 6 Loi 03/10/2022).</li>
 *   <li><b>Refus motivé écrit dans le mois</b> — art. 5 § 5 :
 *       l'employeur dispose d'1 mois pour notifier un refus motivé
 *       par écrit. Le silence ne vaut <b>pas</b> accord tacite, mais
 *       le défaut de motivation expose à l'action en abus de droit.</li>
 *   <li><b>Protection contre le licenciement</b> — art. 5 § 6 : le
 *       travailleur qui demande le bénéfice du régime est protégé
 *       contre tout licenciement motivé par cette demande (charge
 *       de la preuve à l'employeur, indemnité de protection = 6 mois
 *       de rémunération).</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #CONFORME_REGIME_4_JOURS_VALIDE} — semaine de 4 jours
 *       valablement mise en place (toutes conditions cumulatives
 *       remplies).</li>
 *   <li>{@link #NON_ELIGIBLE_TEMPS_PARTIEL} — travailleur à temps
 *       partiel : régime non ouvert (art. 5 § 1).</li>
 *   <li>{@link #NON_CONFORME_DEMANDE_ECRITE_MANQUANTE} — pas de
 *       demande écrite du travailleur (art. 5 § 2) → mise en place
 *       irrégulière, requalifiable en horaire imposé.</li>
 *   <li>{@link #NON_CONFORME_JOURNEE_DEPASSE_9H30} — journée
 *       quotidienne &gt; 9 h 30 sans CCT autorisant 10 h (art. 5
 *       § 3) → violation des limites quotidiennes, sursalaire
 *       éventuel + action SPF ETCS.</li>
 *   <li>{@link #NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT} —
 *       absence d'avenant écrit au contrat ou de modification du
 *       règlement de travail (art. 5 § 4 + art. 6).</li>
 *   <li>{@link #NON_CONFORME_DUREE_DEPASSE_6_MOIS} — durée de
 *       l'avenant supérieure à 6 mois sans renouvellement explicite
 *       (art. 5 § 4) → fragilité contractuelle.</li>
 *   <li>{@link #REFUS_EMPLOYEUR_NON_MOTIVE} — l'employeur a refusé
 *       sans motivation écrite : exposition à l'action en abus de
 *       droit (art. 5 § 5).</li>
 *   <li>{@link #LICENCIEMENT_REPRESAILLES_PRESUME} — licenciement
 *       intervenu après une demande de semaine de 4 jours sans
 *       motif objectif établi par l'employeur : présomption de
 *       représailles (art. 5 § 6), indemnité de protection = 6 mois
 *       de rémunération.</li>
 *   <li>{@link #A_ANALYSER} — configuration ambigüe (statut
 *       travailleur indéterminé, articulation avec d'autres régimes
 *       horaires) → analyse cas par cas.</li>
 * </ul>
 */
public enum Semaine4JoursBeVerdict {
    /** Toutes conditions cumulatives remplies — régime valide. */
    CONFORME_REGIME_4_JOURS_VALIDE,

    /** Travailleur à temps partiel — exclusion art. 5 § 1. */
    NON_ELIGIBLE_TEMPS_PARTIEL,

    /** Demande écrite du travailleur manquante — art. 5 § 2. */
    NON_CONFORME_DEMANDE_ECRITE_MANQUANTE,

    /** Journée &gt; 9 h 30 sans CCT 10 h — art. 5 § 3. */
    NON_CONFORME_JOURNEE_DEPASSE_9H30,

    /** Avenant écrit ou règlement de travail manquant — art. 5 § 4 / art. 6. */
    NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT,

    /** Durée avenant &gt; 6 mois sans renouvellement — art. 5 § 4. */
    NON_CONFORME_DUREE_DEPASSE_6_MOIS,

    /** Refus employeur non motivé par écrit — art. 5 § 5. */
    REFUS_EMPLOYEUR_NON_MOTIVE,

    /** Licenciement post-demande sans motif objectif — art. 5 § 6. */
    LICENCIEMENT_REPRESAILLES_PRESUME,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
