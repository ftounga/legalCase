package fr.ailegalcase.casefile;

/**
 * SF-213-07 : taille d'entreprise pour piloter la procédure interne BE de
 * plainte pour harcèlement.
 *
 * <p>Le seuil <b>50 travailleurs</b> est déterminant : au-delà, l'employeur
 * doit obligatoirement disposer d'un CPP (Comité pour la Prévention et la
 * Protection au Travail) et d'un Conseiller en Prévention Aspects
 * Psychosociaux (CPAP) interne ou via un SEPP (Service Externe pour la
 * Prévention et la Protection au Travail). En-dessous, le recours direct
 * via SPF Emploi / Inspection sociale reste possible.</p>
 *
 * <p>Source : Loi du 4 août 1996 + AR 10/04/2014.</p>
 */
public enum HarcelementBeTailleEntrepriseEnum {
    /** Entreprise &lt; 50 travailleurs — pas d'obligation CPP, CPAP via SEPP. */
    MOINS_DE_50,

    /** Entreprise ≥ 50 travailleurs — obligation CPP + CPAP. */
    CINQUANTE_ET_PLUS
}
