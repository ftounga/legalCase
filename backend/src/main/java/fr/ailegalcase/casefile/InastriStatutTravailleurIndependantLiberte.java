package fr.ailegalcase.casefile;

/**
 * SF-219-27 : modalité d'évaluation des 3 critères de liberté de la
 * Loi-programme I du 27/12/2006 art. 333 (liberté d'organisation du
 * temps de travail, liberté d'organisation du travail, exercice d'un
 * contrôle hiérarchique).
 *
 * <p>L'évaluation porte sur la <b>réalité</b> de l'exécution de la
 * relation, peu importe ce qu'en dit la convention (Cass. BE
 * 23/12/2002 S.02.0021.F).</p>
 *
 * <ul>
 *   <li>{@link #TOTALE_INDEPENDANT} — liberté totale ou contrôle
 *       absent : indice d'indépendance.</li>
 *   <li>{@link #PARTIELLE} — liberté partiellement encadrée ou
 *       contrôle ponctuel : indice neutre.</li>
 *   <li>{@link #AUCUNE_SALARIE} — aucune liberté ou contrôle
 *       hiérarchique régulier : indice de salariat.</li>
 * </ul>
 */
public enum InastriStatutTravailleurIndependantLiberte {
    /** Liberté totale ou absence de contrôle — indice indépendant. */
    TOTALE_INDEPENDANT,

    /** Liberté partielle ou contrôle ponctuel — indice neutre. */
    PARTIELLE,

    /** Aucune liberté ou contrôle hiérarchique régulier — indice salarié. */
    AUCUNE_SALARIE
}
