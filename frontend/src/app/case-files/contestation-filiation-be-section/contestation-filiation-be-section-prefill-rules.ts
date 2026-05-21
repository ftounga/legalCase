/**
 * F-217 SF-217-19 — Helper partagé pour l'outil "Contestation de filiation
 * BE" (`contestation-filiation-be`).
 *
 * V1 : aucun flag pivot extrait par le pipeline IA pour cette situation
 * (présomption de paternité litigieuse, qualité du client à agir, date de
 * connaissance du fait remettant en cause la filiation). `PREFILL_COUNT_ALWAYS_ZERO
 * = true` — `computePrefillCount` retourne toujours 0. Factuel, pas une dette
 * masquée (cf. `SF-217-00-coherence.md` ajustement n° 2 ; précédents
 * SF-217-11 / SF-217-12 / SF-217-13). Extension IA = SF ultérieure si signal
 * terrain (branche un éventuel flag `presomption_paternite_litige_be` du
 * pipeline V2 mentionné audit F-191).
 */

export interface ContestationFiliationBePrefillInput {
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
  _input: ContestationFiliationBePrefillInput,
): number {
  return 0;
}

export const ContestationFiliationBeSectionPrefillRules = {
  computePrefillCount,
};
