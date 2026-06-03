/**
 * SF-223-05 — Helper partagé pour l'outil "Régime algérien — reconnaissance
 * mariage / talaq / dot (Belgique)" (`regime-algerien-be`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Trois champs sont factualisables depuis
 * le sous-objet IA `regime_algerien_be_detection` (consolidé dans
 * `Sf223Detail`, @JsonUnwrapped → clés camelCase plates sur
 * `familleExtractedData`) :
 *
 *  - `regimeAlgerienBeNatureActeDetecte` (MARIAGE_ALGERIEN / TALAQ_ALGERIEN /
 *    DOT_MAHR) → champ `natureActe` ;
 *  - `regimeAlgerienBeDateActeDetectee` (ISO YYYY-MM-DD) → champ `dateActe` ;
 *  - `regimeAlgerienBeMontantDotDetecte` (chaîne numérique) → champ
 *    `montantDotConnu`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0
 * à 3). Le consentement des époux, l'invocation de la Convention et le lien de
 * rattachement relèvent de l'appréciation de l'avocat et ne sont pas
 * factualisables de manière stable en V1 (laissés à 0). Le static
 * `getPrefillCount` du composant délègue à ce helper (parité runtime/static —
 * garde-fou `prefill-count-integrity`).
 */

interface RegimeAlgerienBeAiShape {
  regimeAlgerienBeNatureActeDetecte?: string | null;
  regimeAlgerienBeDateActeDetectee?: string | null;
  regimeAlgerienBeMontantDotDetecte?: string | null;
}

export interface RegimeAlgerienBePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const NATURE_VALUES = new Set(['MARIAGE_ALGERIEN', 'TALAQ_ALGERIEN', 'DOT_MAHR']);
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Nombre de champs pré-remplis par l'IA (0 à 3) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: RegimeAlgerienBePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as RegimeAlgerienBeAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.regimeAlgerienBeNatureActeDetecte === 'string'
      && NATURE_VALUES.has(ai.regimeAlgerienBeNatureActeDetecte)) {
    count += 1;
  }
  if (typeof ai.regimeAlgerienBeDateActeDetectee === 'string'
      && ISO_DATE.test(ai.regimeAlgerienBeDateActeDetectee)) {
    count += 1;
  }
  if (typeof ai.regimeAlgerienBeMontantDotDetecte === 'string'
      && ai.regimeAlgerienBeMontantDotDetecte.trim() !== ''
      && !Number.isNaN(Number(ai.regimeAlgerienBeMontantDotDetecte))
      && Number(ai.regimeAlgerienBeMontantDotDetecte) >= 0) {
    count += 1;
  }
  return count;
}

export const RegimeAlgerienBeSectionPrefillRules = {
  computePrefillCount,
};
