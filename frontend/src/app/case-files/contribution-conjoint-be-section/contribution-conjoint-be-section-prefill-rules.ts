/**
 * SF-217-09 — Helper partagé pour l'outil "Pension alimentaire entre ex-époux
 * (Belgique)" (`contribution-conjoint-be`). Module pur — runtime et static
 * appellent les mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * `PREFILL_COUNT_ALWAYS_ZERO` : en V1 le pipeline IA n'extrait AUCUN flag
 * pivot dédié à la pension entre ex-époux (type de divorce, état de besoin,
 * revenus, durée du mariage). Aucun champ du formulaire n'est donc pré-rempli
 * depuis l'analyse — `computePrefillCount` retourne 0 de manière
 * inconditionnelle. État factuel documenté (pas une dette masquée, cf.
 * SF-217-00) : l'extension IA fera l'objet d'une SF ultérieure si le signal
 * terrain émerge.
 */

/** Constante de documentation : aucun pré-fill IA en V1. */
export const PREFILL_COUNT_ALWAYS_ZERO = true;

export interface ContributionConjointBePrefillInput {
  aiData?: unknown;
}

/**
 * V1 — aucun champ extrait par le pipeline IA → toujours 0.
 * Le paramètre est conservé pour la parité de signature avec les autres
 * helpers `*PrefillRules`.
 */
export function computePrefillCount(
  _input: ContributionConjointBePrefillInput,
): number {
  return 0;
}

export const ContributionConjointBeSectionPrefillRules = {
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
};
