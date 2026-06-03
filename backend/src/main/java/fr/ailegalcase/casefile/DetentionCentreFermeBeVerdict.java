package fr.ailegalcase.casefile;

/**
 * SF-221-04 : verdict de l'analyse de détention en centre fermé + requête de mise en
 * liberté (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 ; AR 02/08/2002 ;
 * requête devant la chambre du conseil art. 71 et s.).
 *
 * <ul>
 *   <li>DETENTION_EN_COURS : maintien en centre fermé constaté, sans fenêtre de requête
 *       active connue — affiche la durée et rappelle le droit de saisir la chambre du
 *       conseil (verdict par défaut).</li>
 *   <li>REQUETE_OUVERTE : la décision de détention a été notifiée, la fenêtre de 5 jours
 *       pour saisir la chambre du conseil est encore ouverte et aucune requête n'a été
 *       déposée — indiquer les jours restants.</li>
 *   <li>REQUETE_TARDIVE : la fenêtre indicative de 5 jours est dépassée et aucune requête
 *       n'a été déposée — la recevabilité est compromise (à vérifier par avocat).</li>
 *   <li>REQUETE_DEPOSEE : une requête de mise en liberté a déjà été introduite devant la
 *       chambre du conseil.</li>
 *   <li>PROLONGATION_A_CONTESTER : une prolongation de la détention a été notifiée — une
 *       nouvelle fenêtre de requête s'ouvre depuis la date de prolongation.</li>
 * </ul>
 */
public enum DetentionCentreFermeBeVerdict {
    DETENTION_EN_COURS,
    REQUETE_OUVERTE,
    REQUETE_TARDIVE,
    REQUETE_DEPOSEE,
    PROLONGATION_A_CONTESTER
}
