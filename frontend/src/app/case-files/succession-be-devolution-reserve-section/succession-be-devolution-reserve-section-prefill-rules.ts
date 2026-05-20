/**
 * F-217 SF-217-13 — Helper partagé pour l'outil "Succession BE — dévolution
 * et réserve héréditaire" (`succession-be-devolution-reserve`).
 *
 * V1 : aucun flag pivot extrait par le pipeline IA pour les successions BE
 * (date du décès, masse successorale, libéralités consenties, présence
 * d'enfants vivants / prédécédés). `PREFILL_COUNT_ALWAYS_ZERO = true` —
 * `computePrefillCount` retourne toujours 0. Factuel, pas une dette masquée
 * (cf. `SF-217-00-coherence.md` ajustement n° 2 ; précédents SF-211-05 /
 * SF-217-03 / SF-217-05). Extension IA = SF ultérieure si signal terrain
 * (branche un éventuel `succession_be_detection` du pipeline V2).
 */

export interface SuccessionBeDevolutionReservePrefillInput {
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
  _input: SuccessionBeDevolutionReservePrefillInput,
): number {
  return 0;
}

export const SuccessionBeDevolutionReserveSectionPrefillRules = {
  computePrefillCount,
};
