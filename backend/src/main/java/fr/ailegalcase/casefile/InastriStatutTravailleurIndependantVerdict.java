package fr.ailegalcase.casefile;

/**
 * SF-219-27 : verdict de l'outil <i>INASTI — statut travailleur
 * indépendant et qualification salarié/indépendant</i> (Loi du
 * 27/06/1969 + AR n° 38 du 27/07/1967 + Loi-programme I du 27/12/2006
 * art. 328 à 333 « doctrine Bart Buysse » + art. 337/1 à 337/3
 * critères sectoriels).
 *
 * <ul>
 *   <li>{@link #INDEPENDANT_CONFIRME} — qualification d'indépendant
 *       confirmée : les 4 critères généraux art. 333 (volonté des
 *       parties, liberté d'organisation du temps, liberté
 *       d'organisation du travail, absence de contrôle hiérarchique)
 *       et, le cas échéant, les critères sectoriels art. 337/2 sont
 *       compatibles avec une relation d'indépendance économique
 *       autonome.</li>
 *   <li>{@link #SALARIE_REQUALIFIE} — qualification d'indépendant
 *       écartée : la majorité des critères pointe vers la
 *       subordination juridique ; relation à requalifier en contrat
 *       de travail salarié (Loi du 03/07/1978) avec rappel
 *       rétroactif des cotisations ONSS depuis l'origine,
 *       éventuellement amende ONSS forfaitaire 3 × (Loi du
 *       22/04/2003 art. 28) et sanction pénale art. 181 C. pén. soc.
 *       niveau 4 si absence DIMONA cumulée.</li>
 *   <li>{@link #FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE} — la
 *       présomption sectorielle de salariat art. 337/2 § 1 est
 *       déclenchée (secteur visé + majorité des 9 critères
 *       sectoriels indicateurs de salariat) ; charge probatoire
 *       inversée à charge de la personne qui invoque
 *       l'indépendance (Cour const. arrêt 167/2013 du 19/12/2013).</li>
 *   <li>{@link #PRESUMPTION_GENERALE_SALARIAT} — la présomption
 *       générale de salariat art. 328 § 1 C. pén. soc. est
 *       mobilisable parce qu'aucune DIMONA n'a été effectuée et
 *       que la personne se déclare sans statut clair (présomption
 *       simple, renversable par tout moyen de preuve — Cass. BE
 *       06/12/2010 S.10.0067.F).</li>
 *   <li>{@link #A_QUALIFIER} — inputs insuffisants, critères
 *       contradictoires, ou nature de la relation à clarifier par
 *       l'avocat avant prise de position définitive.</li>
 * </ul>
 *
 * <h2>Articulation avec SF-219-25 (auditorat) et SF-219-26 (DIMONA)</h2>
 * <p>SF-219-27 qualifie la <b>nature de la relation</b> (salarié /
 * indépendant) ; SF-219-26 quantifie les conséquences
 * <b>matérielles</b> du défaut DIMONA (cotisations rétroactives,
 * amende ONSS, art. 181) si une requalification salariat est
 * retenue ; SF-219-25 oriente vers la <b>procédure</b> (auditorat
 * du travail) si une infraction pénale sociale est suspectée. Les
 * trois outils sont complémentaires et fonctionnent en chaîne :
 * SF-219-27 (qualification nature) → SF-219-26 (chiffrage passif
 * ONSS) → SF-219-25 (orientation auditorat le cas échéant).</p>
 */
public enum InastriStatutTravailleurIndependantVerdict {
    /** Indépendant confirmé — critères compatibles autonomie économique. */
    INDEPENDANT_CONFIRME,

    /** Salarié requalifié — critères majoritaires de subordination. */
    SALARIE_REQUALIFIE,

    /** Faux indépendant par présomption sectorielle art. 337/2. */
    FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE,

    /** Présomption générale de salariat art. 328 mobilisable. */
    PRESUMPTION_GENERALE_SALARIAT,

    /** À qualifier — analyse complémentaire avocat requise. */
    A_QUALIFIER
}
