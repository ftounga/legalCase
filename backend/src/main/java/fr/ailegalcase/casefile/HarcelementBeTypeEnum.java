package fr.ailegalcase.casefile;

/**
 * SF-213-07 : type de harcèlement détecté en droit belge.
 *
 * <p>Source : Loi du 4 août 1996 relative au bien-être des travailleurs lors
 * de l'exécution de leur travail, art. 32bis–32sexies — définit le
 * harcèlement moral, le harcèlement sexuel et la violence au travail.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (référent
 * harcèlement, médecin du travail) avec des procédures différentes.</p>
 *
 * <ul>
 *   <li>{@link #MORAL} — comportement abusif, répété ou non, portant
 *       atteinte à la dignité, à la personnalité physique ou psychique du
 *       travailleur.</li>
 *   <li>{@link #SEXUEL} — comportement non désiré à connotation sexuelle.</li>
 *   <li>{@link #LES_DEUX} — cumul des deux qualifications dans la même
 *       plainte.</li>
 * </ul>
 */
public enum HarcelementBeTypeEnum {
    /** Harcèlement moral — Loi 04/08/1996 art. 32ter. */
    MORAL,

    /** Harcèlement sexuel — Loi 04/08/1996 art. 32ter. */
    SEXUEL,

    /** Cumul harcèlement moral + sexuel dans la même plainte. */
    LES_DEUX
}
