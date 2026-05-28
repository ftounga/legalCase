package fr.ailegalcase.casefile;

/**
 * SF-219-12 : secteur d'activité de l'employeur candidat au régime
 * flexi-job, tel que progressivement ouvert par les lois successives.
 *
 * <h2>Chronologie des ouvertures sectorielles</h2>
 * <ul>
 *   <li><b>2013 (Loi-programme 26/12/2013, art. 13)</b> — secteur
 *       <b>HoReCa</b> (CP 302) uniquement.</li>
 *   <li><b>2015 (Loi-programme 16/11/2015)</b> — extension
 *       boulangerie / coiffure (CP 119 / CP 314) — annulée par
 *       Cour constitutionnelle arrêt 107/2017 puis rétablie par Loi
 *       25/04/2014 corrigée.</li>
 *   <li><b>2018 (Loi-programme 30/10/2018)</b> — extension <b>commerce
 *       de détail</b> (CP 201 / CP 202 / CP 311 / CP 312), agriculture
 *       (CP 132 / CP 144 / CP 145), soins de santé partiels.</li>
 *   <li><b>2023 (Loi-programme 28/12/2023, AR 02/06/2024)</b> —
 *       extension secteurs publics limités, sport (CP 223), culture
 *       (CP 304), événementiel, déménagement (CP 140.05), garages
 *       (CP 112), transport autocar (CP 140.01), assurances limité.</li>
 * </ul>
 *
 * <p>L'énumération couvre les <b>familles</b> de secteurs éligibles ; les
 * commissions paritaires exactes sont à confirmer par avocat BE selon
 * l'activité réelle de l'employeur.</p>
 */
public enum FlexiJobBeSecteurEmployeur {
    /** CP 302 — HoReCa (régime historique 2013). */
    HORECA_302,

    /** CP 119 / CP 314 — boulangerie / coiffure (extension 2015). */
    BOULANGERIE_COIFFURE,

    /** CP 201/202/311/312 — commerce de détail (extension 2018). */
    COMMERCE_DETAIL,

    /** CP 132/144/145 — agriculture et horticulture (extension 2018). */
    AGRICULTURE,

    /** Soins de santé partiels (extension 2018, périmètre limité). */
    SOINS_DE_SANTE,

    /** Secteurs publics éligibles (extension 2023, périmètre limité). */
    SECTEUR_PUBLIC,

    /** Sport, culture, événementiel, transport, garages (extension 2023). */
    SPORT_CULTURE_EVENEMENTIEL,

    /** Secteur non listé par les lois-programmes 2013/2015/2018/2023. */
    AUTRE_NON_ELIGIBLE,

    /**
     * Extension récente sans AR d'application connu / zone grise — analyse
     * juridique requise pour conclure (avocat BE droit social).
     */
    ZONE_GRISE_EXTENSION_RECENTE
}
