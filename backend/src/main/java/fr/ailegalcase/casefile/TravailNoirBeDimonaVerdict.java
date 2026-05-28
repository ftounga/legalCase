package fr.ailegalcase.casefile;

/**
 * SF-219-26 : verdict de l'outil <i>Travail noir BE — DIMONA,
 * requalification et sanctions</i> (Loi-programme du 24/12/2002 art.
 * 167-184 + AR du 05/11/2002 + Code pénal social art. 181 niveau 4).
 *
 * <ul>
 *   <li>{@link #DIMONA_CONFORME} — DIMONA effectuée avant le début
 *       effectif de l'occupation. Aucune sanction. Rappel obligation
 *       de tenir le DRS et la DmfA trimestrielle ONSS.</li>
 *   <li>{@link #DIMONA_TARDIVE_REGULARISABLE} — DIMONA effectuée
 *       après le début effectif mais avant le contrôle inspection
 *       sociale — la situation est partiellement régularisable.
 *       Sanction administrative ONSS possible (amende forfaitaire
 *       réduite) mais pas niveau 4 pénal art. 181 C. pén. soc.</li>
 *   <li>{@link #ABSENCE_DIMONA_SANCTION_NIVEAU_4} — aucune DIMONA au
 *       moment du contrôle — infraction art. 181 C. pén. soc. niveau
 *       4 (amende admin 300 à 3000 € ou pénale 600 à 6000 € ET/OU
 *       emprisonnement 6 mois à 3 ans) cumulée avec cotisations
 *       ONSS rétroactives (employeur ~25 % + travailleur 13,07 %)
 *       et amende ONSS forfaitaire = 3 × les cotisations dues
 *       (art. 28 Loi du 22/04/2003).</li>
 *   <li>{@link #REQUALIFICATION_SALARIE_PRESUMEE} — présomption de
 *       salariat art. 328 § 1 C. pén. soc. déclenchée par l'absence
 *       de DIMONA + éléments de subordination caractérisés —
 *       relation requalifiée en salariat avec rappel cotisations
 *       ONSS rétroactives depuis l'origine + sanction art. 181.</li>
 *   <li>{@link #INDEPENDANT_REQUALIFIE_NON_DECLARE} — relation
 *       faussement qualifiée d'indépendante (faux indépendant)
 *       requalifiée en salariat : obligation DIMONA depuis
 *       l'origine non remplie, sanction art. 181 + cotisations
 *       ONSS rétroactives depuis le début de la relation
 *       (Loi-programme I du 27/12/2006 art. 328 et s. critères
 *       Bart Buysse pour les secteurs construction / transport /
 *       nettoyage / agriculture / horeca / gardiennage).</li>
 *   <li>{@link #A_QUALIFIER} — inputs insuffisants ou ambigus —
 *       élément de subordination INDETERMINE ou cohérence
 *       dates/statut à clarifier par l'avocat.</li>
 * </ul>
 *
 * <h2>Articulation avec SF-219-24 (Code pénal social BE)</h2>
 * <p>L'outil SF-219-26 calcule les conséquences <b>matérielles</b>
 * du défaut DIMONA (cotisations rétroactives, amende ONSS
 * forfaitaire 3×, sanction pénale art. 181). L'outil SF-219-24
 * qualifie l'infraction au regard du barème général à 4 niveaux
 * du Code pénal social et restitue les bornes d'amende génériques.
 * Les deux outils sont complémentaires : SF-219-26 chiffre le
 * passif ONSS et la sanction art. 181 spécifique au défaut DIMONA ;
 * SF-219-24 qualifie l'infraction sur l'échelle 1-4 et permet
 * d'estimer la fourchette d'amende administrative ou pénale après
 * indexation par les décimes additionnels.</p>
 */
public enum TravailNoirBeDimonaVerdict {
    /** DIMONA conforme — déclaration faite avant début occupation. */
    DIMONA_CONFORME,

    /** DIMONA tardive — déclaration faite après début mais avant contrôle. */
    DIMONA_TARDIVE_REGULARISABLE,

    /** Absence DIMONA + sanction niveau 4 art. 181 C. pén. soc. */
    ABSENCE_DIMONA_SANCTION_NIVEAU_4,

    /** Présomption de salariat art. 328 C. pén. soc. déclenchée. */
    REQUALIFICATION_SALARIE_PRESUMEE,

    /** Faux indépendant requalifié — DIMONA jamais effectuée. */
    INDEPENDANT_REQUALIFIE_NON_DECLARE,

    /** Inputs insuffisants ou ambigus — analyse complémentaire avocat. */
    A_QUALIFIER
}
