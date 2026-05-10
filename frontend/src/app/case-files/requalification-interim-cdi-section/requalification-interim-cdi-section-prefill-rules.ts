/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Requalification Intérim → CDI" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface RequalificationInterimCdiPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: RequalificationInterimCdiPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: RequalificationInterimCdiPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  return count;
}

export const RequalificationInterimCdiSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computePrefillCount,
};
