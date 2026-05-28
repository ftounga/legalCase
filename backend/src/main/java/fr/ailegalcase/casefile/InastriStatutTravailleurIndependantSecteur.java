package fr.ailegalcase.casefile;

/**
 * SF-219-27 : secteur d'activité économique pour l'application de la
 * présomption sectorielle de salariat de la Loi-programme I du
 * 27/12/2006 art. 337/1 à 337/3 (introduite par la Loi du 25/08/2012
 * modifiant l'art. 337/2).
 *
 * <p>Pour les secteurs listés à l'art. 337/2, la qualification
 * d'indépendant est <b>présumée renversée</b> lorsque plus de la
 * moitié des 9 critères sectoriels indique le salariat (présomption
 * réfragable, Cour const. arrêt 167/2013 du 19/12/2013 conformité
 * de la liste sectorielle).</p>
 *
 * <ul>
 *   <li>{@link #CONSTRUCTION} — AR du 07/06/2013 — 9 critères
 *       sectoriels art. 337/2 § 1, 1° (immobilisations, statut
 *       gérant, exclusivité, etc.).</li>
 *   <li>{@link #TRANSPORT_DE_CHOSES} — secteur visé art. 337/2
 *       § 1, 2° (Loi-programme I 27/12/2006 modifiée).</li>
 *   <li>{@link #GARDIENNAGE} — secteur visé art. 337/2 § 1, 3°
 *       (sécurité privée et gardiennage).</li>
 *   <li>{@link #NETTOYAGE} — secteur visé art. 337/2 § 1, 4°.</li>
 *   <li>{@link #AGRICULTURE_HORTICULTURE} — secteur visé art. 337/2
 *       § 1, 5° (introduit par AR du 29/10/2013).</li>
 *   <li>{@link #AUTRE} — secteur non visé : application des 4
 *       critères généraux art. 333 sans présomption sectorielle.</li>
 * </ul>
 */
public enum InastriStatutTravailleurIndependantSecteur {
    /** Construction — 9 critères sectoriels art. 337/2 § 1, 1°. */
    CONSTRUCTION,

    /** Transport de choses pour le compte de tiers. */
    TRANSPORT_DE_CHOSES,

    /** Gardiennage et sécurité privée. */
    GARDIENNAGE,

    /** Nettoyage. */
    NETTOYAGE,

    /** Agriculture et horticulture. */
    AGRICULTURE_HORTICULTURE,

    /** Secteur non visé par la présomption sectorielle. */
    AUTRE
}
