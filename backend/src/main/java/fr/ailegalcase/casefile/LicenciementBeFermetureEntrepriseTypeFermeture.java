package fr.ailegalcase.casefile;

/**
 * SF-219-06 : type de fermeture déclarée par l'employeur — détermine
 * l'éligibilité aux régimes FFE (Loi 26/06/2002, AR 23/03/2007).
 *
 * <p>Seules certaines fermetures qualifient pour le Fonds — les
 * cessations partielles, les transferts d'entreprise (CCT n° 32bis du
 * 07/06/1985) et les fusions sans suppression nette de postes ne
 * donnent pas droit à l'indemnité ni à la reprise des créances FFE.</p>
 *
 * <ul>
 *   <li>{@link #CESSATION_DEFINITIVE} — cessation totale et définitive
 *       de l'activité : qualifie pleinement pour le FFE (cas standard).</li>
 *   <li>{@link #FAILLITE_LIQUIDATION_JUDICIAIRE} — faillite judiciaire ou
 *       liquidation : qualifie + insolvabilité présumée → FFE en reprise
 *       de créances.</li>
 *   <li>{@link #CESSATION_PARTIELLE} — cessation d'une partie de
 *       l'activité (ex. une division, un département) — ne qualifie pas
 *       pour le FFE général V1.</li>
 *   <li>{@link #TRANSFERT_CCT_32BIS} — transfert d'entreprise au sens
 *       de la CCT 32bis : les contrats sont repris de plein droit, pas
 *       de fermeture FFE.</li>
 *   <li>{@link #FUSION_SANS_SUPPRESSION} — fusion par absorption sans
 *       suppression nette d'emplois — pas de fermeture qualifiante.</li>
 *   <li>{@link #FERMETURE_TEMPORAIRE} — fermeture temporaire (chômage
 *       technique, force majeure) — pas d'éligibilité FFE.</li>
 * </ul>
 */
public enum LicenciementBeFermetureEntrepriseTypeFermeture {
    /** Cessation totale et définitive de l'activité — qualifie pleinement. */
    CESSATION_DEFINITIVE,

    /** Faillite judiciaire ou liquidation — qualifie + insolvabilité présumée. */
    FAILLITE_LIQUIDATION_JUDICIAIRE,

    /** Cessation d'une partie de l'activité — non qualifiant V1. */
    CESSATION_PARTIELLE,

    /** Transfert d'entreprise au sens CCT 32bis — non qualifiant. */
    TRANSFERT_CCT_32BIS,

    /** Fusion par absorption sans suppression d'emplois — non qualifiant. */
    FUSION_SANS_SUPPRESSION,

    /** Fermeture temporaire (chômage technique, force majeure) — non qualifiant. */
    FERMETURE_TEMPORAIRE
}
