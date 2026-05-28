package fr.ailegalcase.casefile;

/**
 * SF-219-22 : statut du rapport d'analyse biennal sur la structure de
 * rémunération H/F (Loi du 22/04/2012 art. 2 + AR du 25/04/2014
 * — formulaires standardisés EBES) que l'employeur ≥ 50 travailleurs
 * doit déposer tous les deux exercices.
 *
 * <p>Le statut aiguille le verdict :</p>
 * <ul>
 *   <li>{@link #DEPOSE_FORMULAIRE_COMPLET} — rapport biennal déposé au
 *       Conseil d'entreprise (ou à défaut à la délégation syndicale)
 *       sur le formulaire officiel complet (AR 25/04/2014 ann. I
 *       formulaire détaillé ≥ 100 ETP ou ann. II formulaire simplifié
 *       50-99 ETP) — la voie privilégiée par la loi.</li>
 *   <li>{@link #DEPOSE_FORMULAIRE_INCOMPLET} — rapport déposé mais
 *       sections manquantes (ventilation par niveau de fonction,
 *       ancienneté, qualifications, classification non détaillée).
 *       Non-conformité matérielle de l'art. 4 AR 17/08/2013.</li>
 *   <li>{@link #EN_PREPARATION} — exercice biennal en cours, rapport
 *       pas encore déposé mais collecte en cours dans les délais
 *       (rapport dû dans les 3 mois de la clôture de l'exercice
 *       comptable suivant la période biennale).</li>
 *   <li>{@link #NON_DEPOSE_DELAI_DEPASSE} — délai de dépôt dépassé
 *       sans rapport — manquement frontal à l'obligation art. 2
 *       Loi 22/04/2012 + sanction C. pén. social art. 195/1.</li>
 *   <li>{@link #INDETERMINE} — pas encore d'instruction (analyse cas
 *       par cas).</li>
 * </ul>
 */
public enum EgaliteFemmesHommesBeStatutRapport {
    /** Rapport biennal déposé au CE / DS sur formulaire officiel complet. */
    DEPOSE_FORMULAIRE_COMPLET,

    /** Rapport déposé mais sections manquantes — non-conformité matérielle. */
    DEPOSE_FORMULAIRE_INCOMPLET,

    /** Exercice biennal en cours, dépôt dans les délais à venir. */
    EN_PREPARATION,

    /** Délai de dépôt dépassé sans rapport — manquement frontal. */
    NON_DEPOSE_DELAI_DEPASSE,

    /** Statut non encore déterminé — analyse cas par cas. */
    INDETERMINE
}
