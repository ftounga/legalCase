package fr.ailegalcase.casefile;

/**
 * SF-219-22 : verdict de l'outil <i>égalité salariale femmes/hommes
 * BE</i> (Loi du 22/04/2012 visant à lutter contre l'écart salarial
 * entre hommes et femmes M.B. 28/08/2012 + AR du 17/08/2013 portant
 * exécution de l'art. 2 + AR du 25/04/2014 modifiant les formulaires
 * de rapport).
 *
 * <h2>Conditions cumulatives pour qu'un employeur BE soit en
 * conformité avec l'obligation rapport égalité H/F</h2>
 * <ol>
 *   <li><b>Seuil 50 travailleurs</b> — art. 2 § 1er Loi 22/04/2012 :
 *       l'obligation pèse sur les employeurs occupant en moyenne au
 *       moins 50 travailleurs (calcul ETP, Loi 04/12/2007 art. 7).
 *       En deçà, l'obligation rapport biennal est inexistante (mais
 *       l'interdiction de discrimination salariale art. 12 reste
 *       applicable).</li>
 *   <li><b>Rapport biennal d'analyse déposé</b> — l'employeur doit
 *       établir tous les deux exercices comptables une analyse
 *       détaillée de la structure de rémunération H/F sur le
 *       formulaire standardisé (AR 25/04/2014 ann. I ≥ 100 ETP, ann.
 *       II 50-99 ETP) et la déposer auprès du Conseil d'entreprise
 *       (à défaut, de la délégation syndicale). Dépôt central
 *       complémentaire EBES auprès de la BNB.</li>
 *   <li><b>Contenu complet ventilation</b> — le rapport doit ventiler
 *       les rémunérations par : (a) niveau de fonction (classification
 *       conventionnelle), (b) ancienneté, (c) niveau de qualification,
 *       (d) régime de travail (temps plein / temps partiel), (e) éléments
 *       composant la rémunération (salaire de base, primes, avantages
 *       complémentaires). Art. 4 AR 17/08/2013 — chaque section est
 *       obligatoire dès lors qu'elle est applicable au profil de
 *       l'entreprise.</li>
 *   <li><b>Plan d'action écart constaté</b> — si l'analyse révèle un
 *       écart de rémunération non justifié par des facteurs objectifs
 *       (art. 5 Loi 22/04/2012), l'employeur établit dans le délai
 *       fixé par CCT sectorielle (ou à défaut dans les 6 mois) un
 *       plan d'action concret avec objectifs chiffrés et calendrier
 *       de mise en œuvre. Suivi annuel au CE.</li>
 *   <li><b>Médiateur facultatif</b> — désignation d'un médiateur dans
 *       l'entreprise (art. 14 Loi 22/04/2012 + CCT n° 25 du
 *       15/10/1975 sur l'égalité de rémunération H/F) compétent pour
 *       recevoir les plaintes individuelles, dialoguer avec
 *       l'employeur et tenter une conciliation avant tout
 *       contentieux. Procédure interne souple, ne se substitue pas
 *       à l'action judiciaire.</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #HORS_CHAMP_SEUIL_NON_ATTEINT} — employeur &lt; 50
 *       travailleurs : obligation rapport biennal non applicable.</li>
 *   <li>{@link #CONFORME_RAPPORT_PLAN_COMPLETS} — rapport biennal
 *       déposé complet + plan d'action si écart, ou pas d'écart
 *       constaté.</li>
 *   <li>{@link #NON_CONFORME_RAPPORT_INCOMPLET} — rapport déposé
 *       mais sections manquantes (ventilation incomplète).</li>
 *   <li>{@link #NON_CONFORME_PLAN_ACTION_MANQUANT} — rapport complet
 *       révèle un écart, mais aucun plan d'action n'a été établi
 *       dans les délais.</li>
 *   <li>{@link #MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE} — délai de
 *       dépôt dépassé, aucun rapport : manquement frontal exposant
 *       l'employeur aux sanctions C. pén. social art. 195/1 (amende
 *       administrative 80 à 800 € ou pénale 200 à 2 000 € par
 *       travailleur, plafonnée) et à l'action en cessation par les
 *       organisations syndicales / Institut pour l'égalité des
 *       femmes et des hommes (IEFH).</li>
 *   <li>{@link #A_ANALYSER} — configuration ambigüe (statut
 *       indéterminé, exercice en cours, articulation avec une CCT
 *       sectorielle spécifique) → analyse cas par cas.</li>
 * </ul>
 */
public enum EgaliteFemmesHommesBeVerdict {
    /** Employeur &lt; 50 travailleurs : obligation non applicable. */
    HORS_CHAMP_SEUIL_NON_ATTEINT,

    /** Rapport biennal complet + plan d'action si écart constaté. */
    CONFORME_RAPPORT_PLAN_COMPLETS,

    /** Rapport déposé mais ventilation art. 4 AR 17/08/2013 incomplète. */
    NON_CONFORME_RAPPORT_INCOMPLET,

    /** Écart constaté mais aucun plan d'action dans les délais. */
    NON_CONFORME_PLAN_ACTION_MANQUANT,

    /** Délai de dépôt dépassé sans rapport — manquement frontal. */
    MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
