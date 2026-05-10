/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil PSE.
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `PseSectionComponent.prefillFromAi()` :
 *   `dateProjet ← aiData.dateLicenciement` (1ʳᵉ approximation).
 */

export interface PsePrefillInput {
  aiData?: { dateLicenciement?: string | null } | null;
}

export function computeDateProjet(input: PsePrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const date = ai.dateLicenciement;
  if (typeof date !== 'string' || date.length === 0) return null;
  return date;
}

export function computePrefillCount(input: PsePrefillInput): number {
  let count = 0;
  if (computeDateProjet(input) !== null) count++;
  return count;
}

export const PseSectionPrefillRules = {
  computeDateProjet,
  computePrefillCount,
};
