package fr.ailegalcase.casefile;

/**
 * SF-218-51 : type de trajet professionnel pour l'outil "Temps de trajet /
 * déplacement" (art. L.3121-4 CT ; CJUE C-266/14 « Tyco », F-DT-81). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DOMICILE_TRAVAIL_HABITUEL : trajet domicile ↔ lieu habituel de travail
 *       (n'est pas du temps de travail effectif ; contrepartie due seulement en
 *       cas de dépassement du temps normal de trajet).</li>
 *   <li>DOMICILE_CLIENT_DEPASSEMENT : trajet domicile ↔ client dépassant le temps
 *       normal de trajet (contrepartie due pour la part excédentaire).</li>
 *   <li>ITINERANT_SANS_LIEU_FIXE : salarié itinérant sans lieu de travail fixe ;
 *       le déplacement domicile–premier/dernier client est qualifié de temps de
 *       travail effectif (CJUE C-266/14 « Tyco »).</li>
 * </ul>
 */
public enum TypeTrajet {
    DOMICILE_TRAVAIL_HABITUEL,
    DOMICILE_CLIENT_DEPASSEMENT,
    ITINERANT_SANS_LIEU_FIXE
}
