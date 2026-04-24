/**
 * SF-155-05 : interface générique des alertes de cohérence F-IA-03.
 *
 * Remplace les 7 variantes locales historiques livrées en parallèle
 * 2026-04-24 (`HLNCoherenceAlert`, `InaptitudeCoherenceAlert`,
 * `HsCoherenceAlert`, `OqtfCoherenceAlert`, `OqtfSdCoherenceAlert`,
 * `IM08AnnexeBeCoherenceAlert`, `IM05CoherenceAlert`) qui divergeaient
 * sur la forme (`severity` vs `blocker` vs `level`, présence/absence
 * de `source`, `contributors`, `pieceTexte`, `iaValue`…).
 *
 * Motivation : éviter la **dette de convergence** — toute évolution
 * F-IA-03 se faisait jusqu'ici 7 fois dans 7 composants avec des
 * risques de divergences croissantes.
 *
 * Référence skill `ai-skills/frontend-coherence-audit.md` §5 —
 * pattern canonique `immigration-title-decision-section` (F-IM-05).
 */

/**
 * Sources possibles d'une alerte de cohérence :
 * - `F96` : checklist procédurale (F-96) mentionne un écart avec la saisie avocat.
 * - `QUESTION_IA` : question complémentaire F-IA-02 répondue "oui" sur un critère.
 * - `IA` : analyse directe du dossier (synthèse IA type `*ExtractedData`).
 * - `PIECE_MANQUANTE` : entrée F-145 sur un critère lié au champ.
 * - `MULTI` : ≥ 2 sources ci-dessus convergent sur la même `expectedValue`.
 */
export type CoherenceAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

/**
 * Sévérité d'une alerte :
 * - `CRITICAL` : action avocat impérative (48h OQTF, placement CRA, transfert imminent…).
 * - `WARNING` : divergence à examiner (défaut pour la plupart des cas).
 * - `INFO` : note discrète non-bloquante (ex. salaire brut déduit d'un net ×1,30).
 */
export type CoherenceAlertSeverity = 'CRITICAL' | 'WARNING' | 'INFO';

/**
 * Alerte de cohérence F-IA-03 rattachée à un field éditable du formulaire
 * d'un outil décisionnel.
 *
 * @template F chaîne énumérée des fields audités par le composant (ex:
 *   `'MOTIF' | 'NATIONALITE_UE'` pour F-IM-05).
 */
export interface CoherenceAlert<F extends string> {
  /** Field du form auquel l'alerte se rattache (type enum du composant). */
  field: F;
  /**
   * Source dominante ; quand `contributors.length > 1`, vaut `MULTI`.
   * Sinon égale à l'unique contributor.
   */
  source: CoherenceAlertSource;
  /**
   * Liste des sources ayant contribué à l'alerte — au moins 1 élément,
   * dans l'ordre d'insertion via le builder.
   */
  contributors: CoherenceAlertSource[];
  /** Sévérité métier. Défaut builder : `WARNING`. */
  severity: CoherenceAlertSeverity;
  /**
   * Libellé court affiché dans le badge (ex. "Pays tiers",
   * "120,00 €/h", "Recours déjà formé détecté"). Non vide.
   */
  expectedDisplay: string;
  /**
   * Motif détaillé (tooltip / popover). Concaténation " ET " des
   * `reason` de chaque source contributrice.
   */
  reason: string;
  /**
   * Texte F-145 (pièce manquante) si la source `PIECE_MANQUANTE` a
   * contribué — sinon `null` / omis.
   */
  pieceTexte?: string | null;
}
