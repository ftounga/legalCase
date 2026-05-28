package fr.ailegalcase.casefile;

/**
 * SF-219-11 : type de formation suivie au sens des régimes régionaux
 * de congé-éducation payé (Loi 22/01/1985 régionalisée 2014).
 *
 * <p>Le plafond d'heures de congé applicable dépend du type de
 * formation. Les trois régions utilisent des typologies proches mais
 * pas strictement alignées — la classification ci-dessous offre un
 * vocabulaire commun, mappé aux régimes régionaux dans le
 * {@link CongeEducationPayeRegionChecker}.</p>
 *
 * <ul>
 *   <li>{@link #PROFESSIONNELLE_QUALIFIANTE} — formation
 *       professionnelle directement liée à l'emploi actuel ou futur,
 *       sanctionnée par un titre / certification reconnu (plafond
 *       le plus élevé dans toutes les régions).</li>
 *   <li>{@link #GENERALE} — formation générale non
 *       professionnelle (langues, informatique générale, sciences
 *       humaines) — plafond intermédiaire.</li>
 *   <li>{@link #ENSEIGNEMENT_SUPERIEUR_ALTERNANCE} — études
 *       supérieures suivies en alternance avec activité salariée
 *       (FLA — VOV — 250 h ; WBR / BXL — assimilé à
 *       PROFESSIONNELLE_QUALIFIANTE).</li>
 *   <li>{@link #RECONVERSION_PUBLIC_FRAGILISE} — formation de
 *       reconversion pour publics fragilisés (chercheurs d'emploi
 *       reconvertis, travailleurs peu qualifiés) — régimes
 *       dérogatoires.</li>
 *   <li>{@link #HORS_LISTE_AGREEE} — formation NON présente dans la
 *       liste des formations agréées par la Région — pas de droit
 *       au congé (verdict INELIGIBLE_HORS_FORMATION_AGREEE).</li>
 * </ul>
 */
public enum CongeEducationPayeRegionTypeFormation {
    /** Formation professionnelle qualifiante — plafond le plus élevé. */
    PROFESSIONNELLE_QUALIFIANTE,

    /** Formation générale non professionnelle (langues, info) — plafond intermédiaire. */
    GENERALE,

    /** Enseignement supérieur en alternance (FLA = 250 h VOV). */
    ENSEIGNEMENT_SUPERIEUR_ALTERNANCE,

    /** Reconversion pour public fragilisé — régime dérogatoire. */
    RECONVERSION_PUBLIC_FRAGILISE,

    /** Formation absente de la liste agréée régionale — pas de droit. */
    HORS_LISTE_AGREEE
}
