package fr.ailegalcase.casefile;

/**
 * SF-219-10 : statut de la désignation du délégué syndical (CCT n° 5
 * du 24/05/1971, art. 4 à 6 — modalités de désignation par les
 * organisations syndicales représentatives).
 *
 * <p>La désignation relève exclusivement des <b>organisations syndicales
 * représentatives</b> (CSC, FGTB, CGSLB en BE), pas d'élection par les
 * travailleurs (différence majeure avec les délégués au CE / CPPT).
 * L'employeur n'a aucun droit de regard sur le choix syndical, mais une
 * procédure de notification doit être respectée.</p>
 *
 * <ul>
 *   <li>{@link #DESIGNE_PAR_OS_REPRESENTATIVE} — désignation régulière
 *       par une organisation syndicale représentative + notification
 *       formelle à l'employeur.</li>
 *   <li>{@link #NOTIFICATION_EMPLOYEUR_MANQUANTE} — désigné par une OS
 *       mais notification écrite à l'employeur absente / non probante
 *       (statut fragile).</li>
 *   <li>{@link #PAS_DESIGNE_PAR_OS} — pas de désignation par une OS
 *       représentative (auto-proclamation, élection interne) — pas de
 *       statut CCT 5.</li>
 *   <li>{@link #DESIGNATION_CONTESTEE} — désignation faisant l'objet
 *       d'une contestation pendante (conflit inter-syndical, qualification
 *       de la représentativité) — analyse juridique requise.</li>
 * </ul>
 */
public enum DelegueSyndicalCct5StatutDesignation {
    /** Désignation régulière par une OS représentative + notification employeur. */
    DESIGNE_PAR_OS_REPRESENTATIVE,

    /** Désigné par OS mais notification employeur manquante / non probante. */
    NOTIFICATION_EMPLOYEUR_MANQUANTE,

    /** Pas de désignation par une OS représentative — pas de statut CCT 5. */
    PAS_DESIGNE_PAR_OS,

    /** Désignation contestée (conflit inter-syndical, représentativité) — analyse requise. */
    DESIGNATION_CONTESTEE
}
