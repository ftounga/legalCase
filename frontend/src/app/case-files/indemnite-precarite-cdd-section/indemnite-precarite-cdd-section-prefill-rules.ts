/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Indemnité précarité CDD" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir (FRANCE uniquement) :
 *   `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`
 */

export interface IndemnitePrecariteCddPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelReference(input: IndemnitePrecariteCddPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: IndemnitePrecariteCddPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  return count;
}

export const IndemnitePrecariteCddSectionPrefillRules = {
  computeSalaireMensuelReference,
  computePrefillCount,
};
