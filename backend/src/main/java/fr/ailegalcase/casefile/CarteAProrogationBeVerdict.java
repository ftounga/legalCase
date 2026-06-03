package fr.ailegalcase.casefile;

/**
 * SF-221-01 : verdict de l'analyse de prorogation de la carte A (séjour temporaire BE).
 *
 * <ul>
 *   <li>PROROGEABLE : motif de séjour persiste, conditions initiales toujours réunies
 *       et l'on est dans la fenêtre de dépôt (30-45 j avant expiration).</li>
 *   <li>A_DEPOSER_URGENT : la fenêtre est ouverte / dépassée mais la carte n'est pas
 *       encore expirée et aucune demande n'a été déposée — dépôt à effectuer sans délai.</li>
 *   <li>CONDITIONS_NON_REUNIES : le motif de séjour ne persiste plus OU les conditions
 *       initiales ne sont plus réunies — la prorogation est compromise.</li>
 *   <li>EXPIREE : la carte A est expirée et aucune demande n'a été déposée — risque de
 *       séjour irrégulier.</li>
 *   <li>DEMANDE_DEPOSEE : une demande de prorogation a déjà été déposée.</li>
 * </ul>
 */
public enum CarteAProrogationBeVerdict {
    PROROGEABLE,
    A_DEPOSER_URGENT,
    CONDITIONS_NON_REUNIES,
    EXPIREE,
    DEMANDE_DEPOSEE
}
