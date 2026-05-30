package fr.ailegalcase.casefile;

/**
 * SF-218-15 : qualification du statut de journaliste professionnel
 * (art. L.7111-1 et s. CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONFIRME : journaliste professionnel présumé — détention de la carte
 *       d'identité de journaliste professionnel (CCIJP), qui établit une
 *       présomption de la qualité de journaliste.</li>
 *   <li>A_QUALIFIER : statut à qualifier — absence de carte de presse : la
 *       qualité de journaliste professionnel doit être établie par d'autres
 *       éléments (caractère principal, régulier et rétribué de l'activité).</li>
 * </ul>
 */
public enum JournalisteStatutQualification {
    CONFIRME,
    A_QUALIFIER
}
