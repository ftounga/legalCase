package fr.ailegalcase.casefile;

/**
 * SF-213-08 : verdict de l'outil de protection du délégué syndical / candidat
 * non élu BE — détermine si le licenciement tombe dans la fenêtre de
 * protection légale (Loi du 19 mars 1991 + CCT n° 5).
 *
 * <ul>
 *   <li>{@link #LICENCIEMENT_INTERDIT_SANS_PROCEDURE} — le licenciement
 *       intervient pendant la période de protection : il est interdit sauf
 *       motif grave reconnu (art. 35 Loi 03/07/1978) ou raisons d'ordre
 *       économique/technique reconnues par la juridiction compétente.
 *       L'employeur s'expose à une <b>indemnité forfaitaire</b> de 2 ans
 *       de rémunération (4 ans si circonstances aggravantes / récidive).</li>
 *   <li>{@link #HORS_PERIODE_PROTECTION} — le licenciement intervient hors
 *       de la période de protection : régime du licenciement de droit
 *       commun applicable (préavis, indemnité compensatoire, motif grave).</li>
 * </ul>
 */
public enum LicenciementBeProtectionDelegueeVerdict {
    /** Licenciement dans la fenêtre de protection — interdit sans procédure. */
    LICENCIEMENT_INTERDIT_SANS_PROCEDURE,

    /** Licenciement hors fenêtre de protection — droit commun applicable. */
    HORS_PERIODE_PROTECTION
}
