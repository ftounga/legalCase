/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Fin de mission intérim" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface FinMissionInterimPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelReference(input: FinMissionInterimPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: FinMissionInterimPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  return count;
}

export const FinMissionInterimSectionPrefillRules = {
  computeSalaireMensuelReference,
  computePrefillCount,
};
