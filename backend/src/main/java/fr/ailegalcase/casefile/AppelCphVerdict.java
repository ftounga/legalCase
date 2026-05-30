package fr.ailegalcase.casefile;

/**
 * SF-218-01 : verdict de recevabilité d'un appel d'un jugement du Conseil de
 * prud'hommes (CPH) devant la chambre sociale de la Cour d'appel (art. 538 CPC ;
 * R. 1461-1 CPC). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DELAI_OUVERT : délai d'appel d'un mois en cours, plus de 7 jours
 *       calendaires restants.</li>
 *   <li>DELAI_URGENT : délai d'appel en cours mais 1 à 7 jours restants —
 *       déclaration d'appel à formaliser sans délai.</li>
 *   <li>DELAI_EXPIRE : délai d'un mois expiré — appel irrecevable (sauf
 *       relevé de forclusion exceptionnel).</li>
 *   <li>VOIE_FERMEE : jugement rendu en dernier ressort (taux de compétence
 *       atteint, R. 1462-1 CPC) — pas d'appel possible, seul un pourvoi en
 *       cassation est ouvert (renvoi F-DT-87).</li>
 * </ul>
 */
public enum AppelCphVerdict {
    DELAI_OUVERT,
    DELAI_URGENT,
    DELAI_EXPIRE,
    VOIE_FERMEE
}
