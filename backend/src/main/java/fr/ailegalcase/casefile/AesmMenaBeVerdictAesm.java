package fr.ailegalcase.casefile;

/**
 * SF-215-11 : verdict d'éligibilité à l'Admission Exceptionnelle de Séjour Mineur (AESM)
 * — outil composite F-IM-30-aesm-mena-be, volet AESM.
 *
 * <p>Composé à partir d'un score 0-100 sur 4 critères pondérés + bonus durée scolaire
 * (voir {@link AesmMenaBeCalculator}) :
 * <ul>
 *   <li>{@link #FAVORABLE} : score ≥ 70 — dossier solide, démarche AESM recommandée.</li>
 *   <li>{@link #SOUS_RESERVE} : score 50–69 — démarche recevable mais nécessite renforcement
 *       (recueil de pièces complémentaires, plan de vie OE plus structuré).</li>
 *   <li>{@link #DEFAVORABLE} : score &lt; 50 — démarche AESM peu recommandée en l'état
 *       (cf. {@code criteresNonRemplis} pour le détail des items manquants).</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de F-IM-19 (MNA FR — ordonnance JE +
 * L.435-3 CESEDA), procédure entièrement différente (un outil = une situation, CLAUDE.md
 * {@code feedback_decision_tools_one_per_situation}).
 *
 * <p>Sources : Loi 04/05/2007 relative à la tutelle MENA (Service des Tutelles SPF Justice) ;
 * loi 15/12/1980 art. 9bis (adaptation MENA) ; circulaire OE 15/09/2005 sur l'AESM.
 */
public enum AesmMenaBeVerdictAesm {
    FAVORABLE,
    SOUS_RESERVE,
    DEFAVORABLE
}
