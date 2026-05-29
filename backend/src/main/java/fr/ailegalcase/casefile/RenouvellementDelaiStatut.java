package fr.ailegalcase.casefile;

/**
 * SF-214-13 : statut du délai de dépôt du renouvellement du titre de séjour —
 * 2 mois avant l'expiration (art. R. 433-1 CESEDA).
 *
 * <ul>
 *   <li>EN_AVANCE : il reste plus de 60 jours avant la date optimale de dépôt
 *       (2 mois avant expiration) — marge confortable.</li>
 *   <li>A_DEPOSER : il reste moins de 60 jours avant la date optimale — préparer
 *       le dossier de renouvellement.</li>
 *   <li>A_DEPOSER_URGENT : il reste moins de 30 jours avant la date optimale —
 *       fenêtre courte, action prioritaire pour ne pas dépasser le délai impératif
 *       (1 mois avant expiration).</li>
 *   <li>DEPOSE : la demande de renouvellement a été déposée (dateDepotDossier
 *       renseignée — prioritaire sur les autres états).</li>
 *   <li>EXPIRE : le titre est expiré (today &gt; dateExpirationTitre) sans dépôt —
 *       risque d'interruption des droits / irrégularité du séjour.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum RenouvellementDelaiStatut {
    EN_AVANCE,
    A_DEPOSER,
    A_DEPOSER_URGENT,
    DEPOSE,
    EXPIRE
}
