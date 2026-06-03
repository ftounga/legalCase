/**
 * SF-223-04 — Helper partagé pour l'outil "Situation contentieuse post-GPA —
 * Belgique" (`gpa-be-situation-contentieuse`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Deux champs sont factualisables depuis
 * le sous-objet IA `gpa_be_detection` (consolidé dans `Sf223Detail`,
 * @JsonUnwrapped → clés camelCase plates sur `familleExtractedData`) :
 *
 *  - `gpaBeLieuDetecte` ("BELGIQUE" / "ETRANGER") → champ `gpaRealiseeEnBelgiqueOuEtranger` ;
 *  - `gpaBeLienGenetiqueDetecte` (PERE_INTENTIONNEL / MERE_INTENTIONNELLE / AUCUN / LES_DEUX)
 *    → champ `lienGenetiqueParentIntentionnel`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0
 * à 2). L'acte étranger, le consentement de la mère porteuse et le statut du
 * couple relèvent de l'appréciation de l'avocat et ne sont pas factualisables
 * de manière stable en V1 (laissés à 0). Le static `getPrefillCount` du
 * composant délègue à ce helper (parité runtime/static — garde-fou
 * `prefill-count-integrity`).
 */

interface GpaBeAiShape {
  gpaBeLieuDetecte?: string | null;
  gpaBeLienGenetiqueDetecte?: string | null;
}

export interface GpaBePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const LIEU_VALUES = new Set(['BELGIQUE', 'ETRANGER']);
const LIEN_VALUES = new Set([
  'PERE_INTENTIONNEL',
  'MERE_INTENTIONNELLE',
  'AUCUN',
  'LES_DEUX',
]);

/**
 * Nombre de champs pré-remplis par l'IA (0 à 2) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: GpaBePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as GpaBeAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.gpaBeLieuDetecte === 'string' && LIEU_VALUES.has(ai.gpaBeLieuDetecte)) {
    count += 1;
  }
  if (typeof ai.gpaBeLienGenetiqueDetecte === 'string'
      && LIEN_VALUES.has(ai.gpaBeLienGenetiqueDetecte)) {
    count += 1;
  }
  return count;
}

export const GpaBeSituationContentieuseSectionPrefillRules = {
  computePrefillCount,
};
