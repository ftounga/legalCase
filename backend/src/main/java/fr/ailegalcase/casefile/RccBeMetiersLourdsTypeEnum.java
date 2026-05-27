package fr.ailegalcase.casefile;

/**
 * SF-219-01 : type de métier lourd ouvrant droit au RCC métiers lourds
 * (régime de chômage avec complément d'entreprise, ex-prépension) sous
 * conditions âge ≥ 58 / carrière ≥ 35 / au moins 5 ans en métier lourd
 * dans les 10 dernières années ou 7 ans dans les 15 dernières années.
 *
 * <p>Source : <b>CCT n° 17</b> du 19/12/1974 (Conseil National du Travail —
 * instaurant un régime d'indemnité complémentaire aux travailleurs âgés
 * en cas de licenciement) + <b>AR du 3 mai 2007</b> fixant le RCC,
 * <b>art. 3</b> (métiers lourds 58+/35 ans).</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (pré-retraite
 * supprimée 2010, dispositifs spécifiques amiante / pénibilité C2P). Liste
 * non exhaustive : les annexes AR et arrêtés ministériels en vigueur
 * doivent être vérifiés par l'avocat.</p>
 */
public enum RccBeMetiersLourdsTypeEnum {

    /** Travail en équipes successives comportant des prestations de nuit. */
    EQUIPES_SUCCESSIVES_NUIT,

    /** Travail en interruptions à horaires variables (jour-nuit alternés). */
    INTERRUPTIONS_HORAIRES_VARIABLES,

    /**
     * Travail pénible : manutention manuelle de charges, construction,
     * mines, métiers réputés physiquement éprouvants par les annexes AR.
     */
    TRAVAIL_PENIBLE,

    /**
     * Autre métier lourd non listé explicitement — à justifier par
     * l'avocat en référence aux arrêtés ministériels en vigueur.
     */
    AUTRE
}
