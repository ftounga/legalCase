package fr.ailegalcase.casefile;

/**
 * SF-218-05 : verdict global d'orientation du pourvoi en cassation sociale.
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>POURVOI_RECOMMANDE : délai ouvert + au moins un cas d'ouverture de
 *       force FORTE (ou moyen sérieux identifié) → risque de non-admission
 *       FAIBLE ;</li>
 *   <li>POURVOI_RISQUE : délai ouvert mais cas d'ouverture de force seulement
 *       MOYENNE / FAIBLE → risque de non-admission MODERE ou ELEVE (art. 1014
 *       CPC) ;</li>
 *   <li>POURVOI_DECONSEILLE : délai ouvert mais aucun cas d'ouverture sérieux —
 *       risque de non-admission ELEVE et aucun moyen sérieux identifié ;</li>
 *   <li>DELAI_EXPIRE : le délai de 2 mois est dépassé (art. 612 CPC) — pourvoi
 *       irrecevable.</li>
 * </ul>
 */
public enum PourvoiCassationSocVerdict {
    POURVOI_RECOMMANDE,
    POURVOI_RISQUE,
    POURVOI_DECONSEILLE,
    DELAI_EXPIRE
}
