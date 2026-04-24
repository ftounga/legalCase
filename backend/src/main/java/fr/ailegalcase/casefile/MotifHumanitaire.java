package fr.ailegalcase.casefile;

/**
 * SF-IM-09-03 : motifs humanitaires reconnus au titre de l'article L.435-2 du
 * CESEDA (admission exceptionnelle au séjour pour motifs humanitaires après
 * consultation de la commission du titre de séjour L.432-14).
 *
 * <p>Enum strictement orienté droit français.
 */
public enum MotifHumanitaire {

    /** Crainte personnelle de retour, sans statut d'asile. */
    RISQUES_AU_RETOUR,

    /** Isolement total : aucune famille au pays d'origine. */
    ISOLEMENT_TOTAL,

    /** Victime de violences (femmes, minorités, violences conjugales, etc.). */
    VICTIME_VIOLENCES,

    /** Victime de traite des êtres humains ou proxénétisme (connexe L.425-1). */
    VICTIME_TRAITE,

    /**
     * Situation médicale précaire non couverte par le titre étranger malade (L.425-9).
     * Le titre L.425-9 requiert indisponibilité du traitement au pays ; pour les cas
     * intermédiaires, L.435-2 peut prendre le relais.
     */
    SITUATION_MEDICALE_PRECAIRE_HORS_L425_9,

    /** Autre motif humanitaire — faisceau d'indices à justifier. */
    AUTRE_HUMANITAIRE
}
