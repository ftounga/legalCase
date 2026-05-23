package fr.ailegalcase.casefile;

/**
 * SF-216-23 : type principal de bien donné dans le cadre d'une donation
 * entre époux (art. 1091-1100 Cciv).
 *
 * <p>Utilisé pour affiner l'analyse (valorisation, soumission au régime
 * de la réserve héréditaire, exclusion temporaire selon régime
 * matrimonial). Sans incidence sur le verdict de révocabilité — exclusivement
 * informatif pour la restitution.</p>
 */
public enum BienDonneTypeEnum {
    IMMOBILIER,
    MOBILIER,
    PORTEFEUILLE,
    NUMERAIRE,
    AUTRE
}
