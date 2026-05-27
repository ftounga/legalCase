package fr.ailegalcase.casefile;

/**
 * SF-215-01 : statut du dossier de renouvellement permit unique au regard du
 * délai réglementaire de 60 jours (AR 02/09/2018 art. 23).
 *
 * <ul>
 *   <li>DEPOSE_EN_TEMPS : > 60 jours avant expiration — anticipation confortable.</li>
 *   <li>DANS_DELAI : ≤ 60 jours, > 30 jours — dans la fenêtre réglementaire.</li>
 *   <li>URGENT : ≤ 30 jours — déposer immédiatement, risque de rupture.</li>
 *   <li>EXPIRE : permit déjà expiré — renouvellement classique forclos, traitement irrégulier.</li>
 * </ul>
 */
public enum SinglePermitBeStatutRenouvellement {
    DEPOSE_EN_TEMPS,
    DANS_DELAI,
    URGENT,
    EXPIRE
}
