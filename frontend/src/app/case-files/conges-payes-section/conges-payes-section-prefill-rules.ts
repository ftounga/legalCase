/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Congés payés — indemnité" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface CongesPayesPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null; dateLicenciement?: string | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: CongesPayesPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateRupture(input: CongesPayesPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: CongesPayesPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  return count;
}

export const CongesPayesSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDateRupture,
  computePrefillCount,
};
