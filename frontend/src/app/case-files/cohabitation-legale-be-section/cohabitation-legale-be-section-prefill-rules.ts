/**
 * SF-223-01 — Helper partagé pour l'outil "Régime de la cohabitation légale en
 * Belgique" (`cohabitation-legale-be`).
 *
 * V1 : aucun champ saisissable n'est pré-rempli par l'IA. Les conditions
 * juridiques (deux personnes non mariées, capacité, non déjà lié, domicile
 * commun, logement familial en jeu, mode de dissolution) relèvent de
 * l'appréciation de l'avocat et ne sont pas factualisables de manière stable
 * en V1. `PREFILL_COUNT_ALWAYS_ZERO = true` — `computePrefillCount` retourne
 * toujours 0. Factuel, pas une dette masquée (cf. pattern SF-217-17 /
 * mariage-etranger-be-reconnaissance).
 *
 * Le flag pivot `cohabitationLegaleBeDetectee` (déjà extrait par F-202) et le
 * sous-objet IA `cohabitation_legale_be_detection` (SF-223-01) alimentent la
 * visibilité CONTEXTUAL F-IA-04 et un futur pré-fill, mais aucun champ
 * saisissable n'y est arrimé en V1.
 */

export interface CohabitationLegaleBePrefillInput {
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
  _input: CohabitationLegaleBePrefillInput,
): number {
  return 0;
}

export const CohabitationLegaleBeSectionPrefillRules = {
  computePrefillCount,
};
