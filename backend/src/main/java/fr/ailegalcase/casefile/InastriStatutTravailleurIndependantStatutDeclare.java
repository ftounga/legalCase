package fr.ailegalcase.casefile;

/**
 * SF-219-27 : statut déclaré par la personne au moment de l'analyse.
 *
 * <p>Permet d'orienter le verdict : si {@link #INDEPENDANT_INASTI} et
 * la qualification réelle penche vers le salariat → faux indépendant
 * (présomption art. 328 § 1 C. pén. soc. mobilisable en cas
 * d'absence DIMONA). Si {@link #SALARIE_DIMONA} et la qualification
 * réelle penche vers l'indépendance → faux salarié (rare en
 * pratique, généralement employeur cherchant à externaliser
 * faussement un coût social).</p>
 *
 * <ul>
 *   <li>{@link #INDEPENDANT_INASTI} — déclaré indépendant, affilié
 *       caisse d'assurances sociales pour travailleurs indépendants
 *       (INASTI / CASTI, AR n° 38 du 27/07/1967).</li>
 *   <li>{@link #SALARIE_DIMONA} — déclaré salarié, déclaration
 *       DIMONA à l'ONSS effectuée (Loi-programme 24/12/2002).</li>
 *   <li>{@link #SANS_DECLARATION} — aucune affiliation déclarée
 *       (situation critique : présomption simple de salariat
 *       art. 328 § 1 C. pén. soc. en cas d'absence DIMONA).</li>
 * </ul>
 */
public enum InastriStatutTravailleurIndependantStatutDeclare {
    /** Déclaré indépendant INASTI. */
    INDEPENDANT_INASTI,

    /** Déclaré salarié DIMONA. */
    SALARIE_DIMONA,

    /** Aucune déclaration. */
    SANS_DECLARATION
}
