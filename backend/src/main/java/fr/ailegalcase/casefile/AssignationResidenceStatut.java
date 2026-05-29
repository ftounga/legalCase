package fr.ailegalcase.casefile;

/**
 * SF-214-35 : statut d'une mesure d'assignation à résidence (L. 731-1 CESEDA)
 * au regard de son échéance.
 *
 * <ul>
 *   <li>EN_COURS : échéance future, marge &gt; 15 jours — mesure active.</li>
 *   <li>EXPIRATION_PROCHE : échéance dans moins de 15 jours — renouvellement
 *       ou contestation à anticiper.</li>
 *   <li>EXPIRE : échéance dépassée — la mesure n'a plus de base.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum AssignationResidenceStatut {
    EN_COURS,
    EXPIRATION_PROCHE,
    EXPIRE
}
