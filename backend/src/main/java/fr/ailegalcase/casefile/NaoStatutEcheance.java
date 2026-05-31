package fr.ailegalcase.casefile;

/**
 * SF-218-29 : statut de l'échéance de la prochaine négociation annuelle obligatoire
 * (NAO, F-DT-66). Outil <b>FRANCE UNIQUEMENT</b>. Calculé à partir de la dernière
 * négociation engagée et de la périodicité retenue (12 mois par défaut, jusqu'à
 * 48 mois par accord de méthode, art. L.2242-11 CT).
 *
 * <ul>
 *   <li>A_JOUR : il reste plus de 60 jours avant la prochaine échéance.</li>
 *   <li>ECHEANCE_PROCHE : la prochaine échéance intervient dans 0 à 60 jours.</li>
 *   <li>DEPASSEE : l'échéance est dépassée (jours restants négatifs) — défaut de
 *       négociation susceptible de caractériser un délit d'entrave.</li>
 * </ul>
 */
public enum NaoStatutEcheance {
    A_JOUR,
    ECHEANCE_PROCHE,
    DEPASSEE
}
