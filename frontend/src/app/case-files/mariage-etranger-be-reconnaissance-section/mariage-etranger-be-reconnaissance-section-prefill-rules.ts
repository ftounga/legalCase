/**
 * F-217 SF-217-17 — Helper partagé pour l'outil "Reconnaissance d'un
 * mariage / divorce étranger en Belgique"
 * (`mariage-etranger-be-reconnaissance`).
 *
 * V1 : aucun flag pivot extrait par le pipeline IA pour la reconnaissance
 * d'un acte étranger (nature de l'acte, pays d'origine, date de l'acte,
 * résidence habituelle, nationalité, conformité fond / forme, booleans
 * talaq, convention bilatérale).
 * `PREFILL_COUNT_ALWAYS_ZERO = true` — `computePrefillCount` retourne
 * toujours 0. Factuel, pas une dette masquée (cf. `SF-217-00-coherence.md`
 * ajustement n° 1 ; précédents SF-211-05 / SF-217-03 / SF-217-13).
 * Extension IA = SF ultérieure si signal terrain (branche un éventuel
 * `mariage_etranger_reconnaissance_detecte` du pipeline V2, cf. audit F-191).
 */

export interface MariageEtrangerBeReconnaissancePrefillInput {
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
  _input: MariageEtrangerBeReconnaissancePrefillInput,
): number {
  return 0;
}

export const MariageEtrangerBeReconnaissanceSectionPrefillRules = {
  computePrefillCount,
};
