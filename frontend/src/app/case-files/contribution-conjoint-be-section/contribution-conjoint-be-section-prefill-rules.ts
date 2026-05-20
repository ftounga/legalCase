/**
 * SF-246-28 — Helper partagé pour l'outil "Pension alimentaire entre ex-époux
 * (Belgique)" (`contribution-conjoint-be`). Module pur — runtime et static
 * appellent les mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * SF-246-28 : levée de `PREFILL_COUNT_ALWAYS_ZERO`. Le pipeline IA extrait
 * désormais 3 champs depuis le sous-objet `famille_be_detection_v2` :
 * `dureeMariageAnneesBeDetectee`, `revenuMensuelCreancierBeDetecte`,
 * `revenuMensuelDebiteurBeDetecte`.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface ContributionConjointBePrefillInput {
  aiData?: FamilleExtractedData | null | unknown;
}

/** Durée du mariage en années entières [0, 80] ou null. */
export function computeDureeMariage(
  input: ContributionConjointBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.dureeMariageAnneesBeDetectee;
  if (v == null || v < 0 || v > 80) return null;
  return v;
}

/** Revenu mensuel du créancier (€/mois, ≥ 0) ou null. */
export function computeRevenuCreancier(
  input: ContributionConjointBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.revenuMensuelCreancierBeDetecte;
  if (v == null || v < 0) return null;
  return v;
}

/** Revenu mensuel du débiteur (€/mois, ≥ 0) ou null. */
export function computeRevenuDebiteur(
  input: ContributionConjointBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.revenuMensuelDebiteurBeDetecte;
  if (v == null || v < 0) return null;
  return v;
}

/**
 * Compte le nombre de champs pré-remplis par l'IA pour le badge du panneau.
 * SF-246-28 : 3 champs maximum.
 */
export function computePrefillCount(
  input: ContributionConjointBePrefillInput,
): number {
  return [
    computeDureeMariage(input),
    computeRevenuCreancier(input),
    computeRevenuDebiteur(input),
  ].filter(v => v != null).length;
}

export const ContributionConjointBeSectionPrefillRules = {
  computePrefillCount,
  computeDureeMariage,
  computeRevenuCreancier,
  computeRevenuDebiteur,
};
