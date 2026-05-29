package fr.ailegalcase.casefile;

/**
 * SF-214-37 : statut d'une interdiction du territoire français (ITF) prononcée
 * par un juge pénal comme peine complémentaire (C. pén. 131-30 à 131-30-2), au
 * regard des voies de recours et du relèvement.
 *
 * <ul>
 *   <li>APPEL_POSSIBLE : condamnation non définitive et délai d'appel
 *       correctionnel (10 j, C. pr. pén. 380-1) non expiré.</li>
 *   <li>RECOURS_PRESCRIT : condamnation non définitive mais délai d'appel
 *       expiré — les voies de recours ordinaires sont closes.</li>
 *   <li>RELEVE_POSSIBLE : condamnation définitive et délai minimum de 5 ans
 *       écoulé — une requête en relèvement (C. pén. 702-1) est recevable.</li>
 *   <li>EN_COURS_PURGE : condamnation définitive mais délai de 5 ans non encore
 *       écoulé — la peine est en cours de purge, relèvement non encore recevable.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (peine pénale du droit français).
 */
public enum ItfJudiciaireStatut {
    APPEL_POSSIBLE,
    RECOURS_PRESCRIT,
    RELEVE_POSSIBLE,
    EN_COURS_PURGE
}
