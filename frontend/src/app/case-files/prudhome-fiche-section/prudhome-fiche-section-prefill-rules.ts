/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de la fiche prud'homale.
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `PrudhomeFicheSectionComponent.prefillFromAi()` :
 *   `demandeur.profession ← aiData.poste`.
 */

export interface PrudhomeFichePrefillInput {
  aiData?: { poste?: string | null } | null;
}

export function computeProfession(input: PrudhomeFichePrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const v = ai.poste;
  if (typeof v !== 'string' || v.length === 0) return null;
  return v;
}

export function computePrefillCount(input: PrudhomeFichePrefillInput): number {
  let count = 0;
  if (computeProfession(input) !== null) count++;
  return count;
}

export const PrudhomeFicheSectionPrefillRules = {
  computeProfession,
  computePrefillCount,
};
