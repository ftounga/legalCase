/**
 * SF-223-08 — Helper partagé pour l'outil "Reconnaissance / exequatur d'une
 * décision familiale étrangère (Belgique — DIP)"
 * (`dip-be-reconnaissance-decision-etrangere`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Trois champs sont factualisables depuis
 * le sous-objet IA `dip_reconnaissance_decision_be_detection` (consolidé dans
 * `Sf223Detail`, @JsonUnwrapped → clés camelCase plates sur
 * `familleExtractedData`) :
 *
 *  - `dipReconnaissanceNatureDetectee` (JUGEMENT_ETRANGER_HORS_UE /
 *    MARIAGE_RELIGIEUX_NON_CIVIL) → champ `natureDecision` ;
 *  - `dipReconnaissancePaysDetecte` (ISO 3166-1 alpha-2) → champ `paysOrigine` ;
 *  - `dipReconnaissanceDateDetectee` (ISO YYYY-MM-DD) → champ `dateDecision`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0 à
 * 3). Les booleans de fond (définitivité, droits de la défense, ordre public,
 * fraude, mariage civil préalable) ne sont pas pré-remplis (appréciation
 * juridique, laissés à 0). Le static `getPrefillCount` du composant délègue à ce
 * helper (parité runtime/static — garde-fou `prefill-count-integrity`).
 */

interface DipBeReconnaissanceAiShape {
  dipReconnaissanceNatureDetectee?: string | null;
  dipReconnaissancePaysDetecte?: string | null;
  dipReconnaissanceDateDetectee?: string | null;
}

export interface DipBeReconnaissanceDecisionEtrangerePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const NATURE_VALUES = new Set(['JUGEMENT_ETRANGER_HORS_UE', 'MARIAGE_RELIGIEUX_NON_CIVIL']);
const ISO2 = /^[A-Z]{2}$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Nombre de champs pré-remplis par l'IA (0 à 3) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: DipBeReconnaissanceDecisionEtrangerePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as DipBeReconnaissanceAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.dipReconnaissanceNatureDetectee === 'string'
      && NATURE_VALUES.has(ai.dipReconnaissanceNatureDetectee)) {
    count += 1;
  }
  if (typeof ai.dipReconnaissancePaysDetecte === 'string'
      && ISO2.test(ai.dipReconnaissancePaysDetecte)) {
    count += 1;
  }
  if (typeof ai.dipReconnaissanceDateDetectee === 'string'
      && ISO_DATE.test(ai.dipReconnaissanceDateDetectee)) {
    count += 1;
  }
  return count;
}

export const DipBeReconnaissanceDecisionEtrangereSectionPrefillRules = {
  computePrefillCount,
};
