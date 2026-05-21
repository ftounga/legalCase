/**
 * F-217 SF-217-15 — Helper partagé pour l'outil "Protection du majeur
 * (Belgique)" (`protection-majeur-be`).
 *
 * V1 : aucun flag pivot extrait par le pipeline IA pour la protection du
 * majeur BE (nature d'altération, gravité d'incapacité, mandat extra-judiciaire,
 * déclaration anticipée, environnement familial). `PREFILL_COUNT_ALWAYS_ZERO
 * = true` — `computePrefillCount` retourne toujours 0. Factuel, pas une dette
 * masquée (cf. `SF-217-00-coherence.md` ajustement n° 2 ; précédents
 * SF-217-03 / SF-217-05 / SF-217-13). Extension IA = SF ultérieure si signal
 * terrain (branche un éventuel `protection_majeur_be_detectee` du pipeline V2
 * — mentionné dans l'audit F-191).
 */

export interface ProtectionMajeurBePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

/**
 * V1 : retourne toujours 0 — aucun champ pré-rempli par l'IA pour cet outil.
 * Parité runtime / static garantie via `PREFILL_COUNT_ALWAYS_ZERO = true`.
 */
export function computePrefillCount(
  _input: ProtectionMajeurBePrefillInput,
): number {
  return 0;
}

export const ProtectionMajeurBeSectionPrefillRules = {
  computePrefillCount,
};
