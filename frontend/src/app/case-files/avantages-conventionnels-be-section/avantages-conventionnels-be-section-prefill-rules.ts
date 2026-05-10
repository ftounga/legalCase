/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Avantages conventionnels" (BE).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Gating : workspaceCountry !== 'BELGIQUE' → null (outil mono-pays BE).
 *
 * Logique miroir :
 *   `salaireMensuelBrutEur ← aiData.salaireBrutMensuel > 0`
 */

export interface AvantagesConventionnelsBePrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: AvantagesConventionnelsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: AvantagesConventionnelsBePrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  return count;
}

export const AvantagesConventionnelsBeSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computePrefillCount,
};
