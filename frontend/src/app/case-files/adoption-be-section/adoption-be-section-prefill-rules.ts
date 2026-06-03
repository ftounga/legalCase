/**
 * SF-223-02 — Helper partagé pour l'outil "Recevabilité de l'adoption en
 * Belgique" (`adoption-be`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Deux champs sont factualisables depuis
 * le sous-objet IA `adoption_be_detection` (consolidé dans `Sf223Detail`,
 * @JsonUnwrapped → clés camelCase plates sur `familleExtractedData`) :
 *
 *  - `adoptionBeTypeDetecte` (PLENIERE / SIMPLE / CO_PARENTALE) → champ
 *    `typeAdoption` ;
 *  - `adoptionBeAgrementMentionneBeDetecte` (boolean) → champ `agrementObtenu`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0
 * à 2). Les conditions d'âge / d'écart d'âge / de consentement relèvent de
 * l'appréciation de l'avocat et ne sont pas factualisables de manière stable en
 * V1 (laissées à 0). Le static `getPrefillCount` du composant délègue à ce
 * helper (parité runtime/static — garde-fou `prefill-count-integrity`).
 */

interface AdoptionBeAiShape {
  adoptionBeTypeDetecte?: string | null;
  adoptionBeAgrementMentionneBeDetecte?: boolean | null;
}

export interface AdoptionBePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const TYPES_VALIDES = new Set(['PLENIERE', 'SIMPLE', 'CO_PARENTALE']);

/**
 * Nombre de champs pré-remplis par l'IA (0 à 2) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: AdoptionBePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as AdoptionBeAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.adoptionBeTypeDetecte === 'string'
      && TYPES_VALIDES.has(ai.adoptionBeTypeDetecte)) {
    count += 1;
  }
  if (typeof ai.adoptionBeAgrementMentionneBeDetecte === 'boolean') {
    count += 1;
  }
  return count;
}

export const AdoptionBeSectionPrefillRules = {
  computePrefillCount,
};
