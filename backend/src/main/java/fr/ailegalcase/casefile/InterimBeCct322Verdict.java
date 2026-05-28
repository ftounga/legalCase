package fr.ailegalcase.casefile;

/**
 * SF-219-14 : verdict de l'outil <i>statut intérim BE</i> (Loi du
 * 24/07/1987 + CCT n° 322 du 14/06/2010 du CNT).
 *
 * <h2>Conditions cumulatives V1</h2>
 * <ol>
 *   <li><b>Motif autorisé</b> — art. 1er § 1 / art. 1bis
 *       Loi 24/07/1987 (cf. {@link InterimBeCct322MotifMission}).</li>
 *   <li><b>Pas de remplacement grève / lock-out</b> — art. 19,
 *       interdiction absolue.</li>
 *   <li><b>Durée maximale légale</b> respectée selon le motif :
 *       remplacement = durée suspension + 7 j ; surcroît = 6 mois
 *       renouvelables max 12 mois ; insertion = 9 mois cumulés
 *       max ; travail exceptionnel = AR 11/10/1976 ; artistique
 *       et flux = au cas par cas.</li>
 *   <li><b>Parité salariale</b> — art. 10 : intérimaire perçoit
 *       la même rémunération brute + primes + chèques-repas
 *       qu'un permanent de même qualification chez l'utilisateur.</li>
 *   <li><b>Formalisme cumulatif</b> : contrat de travail écrit ETI
 *       / intérimaire (art. 8 § 1) + contrat de mise à disposition
 *       écrit ETI / utilisateur (art. 9) + déclaration Dimona ETI.</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #ELIGIBLE_MISSION_REGULIERE} — toutes les conditions
 *       cumulatives sont réunies, mission intérimaire <b>régulière</b>.</li>
 *   <li>{@link #INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT} — recours
 *       pour remplacer un travailleur en grève ou lock-out
 *       (art. 19) — interdiction absolue, requalification immédiate
 *       avec présomption irréfragable de fraude.</li>
 *   <li>{@link #INELIGIBLE_MOTIF_NON_AUTORISE} — motif invoqué non
 *       listé par la loi → requalification CDI utilisateur
 *       (art. 20).</li>
 *   <li>{@link #INELIGIBLE_DUREE_MAX_DEPASSEE} — la durée totale de
 *       la mission excède le plafond légal applicable au motif.</li>
 *   <li>{@link #INELIGIBLE_PARITE_SALARIALE_VIOLEE} — le salaire
 *       horaire brut intérimaire est inférieur à celui du
 *       travailleur permanent de référence (art. 10).</li>
 *   <li>{@link #FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — conditions
 *       de fond OK mais contrat écrit / Dimona manquant —
 *       requalification automatique pour défaut de formalisme.</li>
 *   <li>{@link #A_ANALYSER} — motif artistique ou flux exceptionnel
 *       à analyser au cas par cas (durée variable selon AR
 *       sectoriels).</li>
 * </ul>
 */
public enum InterimBeCct322Verdict {
    /** Mission intérimaire régulière — toutes conditions cumulatives réunies. */
    ELIGIBLE_MISSION_REGULIERE,

    /** Statut formellement défaillant (contrat écrit / Dimona) — requalification immédiate. */
    FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,

    /** Recours pour remplacer un travailleur en grève / lock-out — interdiction absolue art. 19. */
    INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT,

    /** Motif invoqué non listé par la loi — requalification CDI utilisateur (art. 20). */
    INELIGIBLE_MOTIF_NON_AUTORISE,

    /** Durée totale de la mission au-delà du plafond légal applicable. */
    INELIGIBLE_DUREE_MAX_DEPASSEE,

    /** Salaire horaire intérimaire sous celui du permanent de référence (art. 10). */
    INELIGIBLE_PARITE_SALARIALE_VIOLEE,

    /** Motif artistique / flux exceptionnel — analyse cas par cas requise. */
    A_ANALYSER
}
