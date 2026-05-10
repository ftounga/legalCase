/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Travail dissimulé".
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface TravailDissimulePrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
}

export function computeSalaireMensuelReference(input: TravailDissimulePrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: TravailDissimulePrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  return count;
}

export const TravailDissimuleSectionPrefillRules = {
  computeSalaireMensuelReference,
  computePrefillCount,
};
