/**
 * SF-DT-36-02 — Helper partagé pour l'outil "Nullité de procédure de
 * licenciement" (F-DT-36). Module pur — runtime et static appellent les mêmes
 * fonctions (contrat F-236 SF-236-01 / F-237 SF-237-01).
 *
 * `PREFILL_COUNT_ALWAYS_ZERO` : en V1 le pipeline IA n'extrait AUCUN flag
 * procédural dédié (délai de convocation, motivation suffisante, etc.). Aucun
 * champ du formulaire n'est donc pré-rempli depuis l'analyse — `getPrefillCount`
 * retourne 0 de manière inconditionnelle. C'est un état factuel documenté (pas
 * une dette masquée) : l'extension IA (flags `delaiConvocationNonRespecte`…)
 * fera l'objet d'une SF ultérieure si le signal terrain émerge.
 */

/** Constante de documentation : aucun pré-fill IA en V1. */
export const PREFILL_COUNT_ALWAYS_ZERO = true;

export interface ProcedureNulliteLicenciementPrefillInput {
  aiData?: unknown;
}

/**
 * V1 — aucun champ procédural extrait par le pipeline IA → toujours 0.
 * Le paramètre est conservé pour la parité de signature avec les autres
 * helpers `*PrefillRules`.
 */
export function computePrefillCount(
  _input: ProcedureNulliteLicenciementPrefillInput,
): number {
  return 0;
}

export const ProcedureNulliteLicenciementSectionPrefillRules = {
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
};
