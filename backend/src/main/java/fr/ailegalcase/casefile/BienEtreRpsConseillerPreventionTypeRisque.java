package fr.ailegalcase.casefile;

/**
 * SF-219-30 : type de risque psychosocial concerné par la saisine du
 * Conseiller en Prévention Aspects Psychosociaux (CPAP) — Loi du
 * 04/08/1996 art. 32sexies + AR du 10/04/2014.
 *
 * <p>La typologie suit l'art. 32/1 § 2 de la Loi du 04/08/1996
 * (introduit par la Loi du 28/02/2014) : les risques psychosociaux
 * couvrent le harcèlement moral, le harcèlement sexuel, la violence
 * au travail et les autres risques psychosociaux (stress, burn-out,
 * conflits interpersonnels qualifiés). La saisine du CPAP est ouverte
 * pour chacune de ces qualifications, avec un régime procédural
 * unique posé par l'AR du 10/04/2014 art. 11 à 32.</p>
 *
 * <ul>
 *   <li>{@link #HARCELEMENT_MORAL} — Loi 04/08/1996 art. 32bis (conduite
 *       abusive et répétée portant atteinte à la personnalité, dignité
 *       ou intégrité physique ou psychique).</li>
 *   <li>{@link #HARCELEMENT_SEXUEL} — Loi 04/08/1996 art. 32bis al. 3
 *       (comportement non désiré à connotation sexuelle).</li>
 *   <li>{@link #VIOLENCE_AU_TRAVAIL} — Loi 04/08/1996 art. 32bis al. 1
 *       (chaque situation de fait où un travailleur est menacé ou
 *       agressé psychiquement ou physiquement).</li>
 *   <li>{@link #AUTRES_RPS} — Loi 04/08/1996 art. 32/1 § 2 (stress,
 *       burn-out, conflits interpersonnels qualifiés, charge mentale).</li>
 * </ul>
 */
public enum BienEtreRpsConseillerPreventionTypeRisque {
    /** Harcèlement moral — Loi 04/08/1996 art. 32bis. */
    HARCELEMENT_MORAL,

    /** Harcèlement sexuel — Loi 04/08/1996 art. 32bis al. 3. */
    HARCELEMENT_SEXUEL,

    /** Violence au travail — Loi 04/08/1996 art. 32bis al. 1. */
    VIOLENCE_AU_TRAVAIL,

    /** Autres risques psychosociaux — Loi 04/08/1996 art. 32/1 § 2. */
    AUTRES_RPS
}
