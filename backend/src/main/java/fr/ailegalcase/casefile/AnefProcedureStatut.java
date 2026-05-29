package fr.ailegalcase.casefile;

/**
 * SF-214-25 : statut de la démarche ANEF / recours en cas de panne du dépôt
 * dématérialisé. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>NORMAL : aucune panne signalée, échéance du titre à plus de 30 jours —
 *       parcours ANEF standard.</li>
 *   <li>URGENT : aucune panne signalée mais échéance du titre à moins de 30 jours
 *       — déposer sans délai pour éviter la rupture de droit au séjour.</li>
 *   <li>PANNE_EN_COURS : panne ANEF signalée, aucune demande encore adressée à la
 *       préfecture — engager la procédure alternative (preuve + LRAR + dépôt
 *       physique).</li>
 *   <li>RECOURS_POSSIBLE : panne ANEF signalée ET demande déjà adressée à la
 *       préfecture — un recours pour faute de l'administration est envisageable en
 *       cas de préjudice (délai 2 ans, responsabilité administrative).</li>
 * </ul>
 */
public enum AnefProcedureStatut {
    NORMAL,
    URGENT,
    PANNE_EN_COURS,
    RECOURS_POSSIBLE
}
