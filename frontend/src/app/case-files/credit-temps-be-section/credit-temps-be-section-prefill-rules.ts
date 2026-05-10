/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Crédit-temps" (BE).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Gating : workspaceCountry !== 'BELGIQUE' → null (outil mono-pays BE).
 *
 * Logique miroir :
 *   `ancienneteEntrepriseMois ← months between dateEntree and now`
 *   `ageDemandeurAnnees       ← aiData.ageDemandeurAnnees (0..75)`
 */

export interface CreditTempsBePrefillInput {
  aiData?: {
    dateEntree?: string | null;
    ageDemandeurAnnees?: number | null;
  } | null;
  workspaceCountry?: string;
  /** Date "now" injectable pour les tests — défaut Date.now(). */
  now?: Date;
}

export function computeAncienneteMois(input: CreditTempsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const dateEntree = input.aiData?.dateEntree;
  if (!dateEntree) return null;
  const d = new Date(dateEntree);
  if (isNaN(d.getTime())) return null;
  const today = input.now ?? new Date();
  if (d > today) return null;
  const months = (today.getFullYear() - d.getFullYear()) * 12
    + (today.getMonth() - d.getMonth());
  return months >= 0 ? months : null;
}

export function computeAgeDemandeur(input: CreditTempsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.ageDemandeurAnnees;
  if (typeof v !== 'number') return null;
  if (v < 0 || v > 75) return null;
  return v;
}

export function computePrefillCount(input: CreditTempsBePrefillInput): number {
  let count = 0;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeAgeDemandeur(input) !== null) count++;
  return count;
}

export const CreditTempsBeSectionPrefillRules = {
  computeAncienneteMois,
  computeAgeDemandeur,
  computePrefillCount,
};
