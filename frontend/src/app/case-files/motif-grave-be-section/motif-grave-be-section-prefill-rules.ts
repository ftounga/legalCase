/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Motif grave" (BE).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Gating : workspaceCountry !== 'BELGIQUE' → null (outil mono-pays BE).
 *
 * Logique miroir :
 *   `dateNotificationRupture ← aiData.dateLicenciement`
 *   `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`
 */

export interface MotifGraveBePrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    salaireBrutMensuel?: number | null;
  } | null;
  workspaceCountry?: string;
}

export function computeDateNotificationRupture(input: MotifGraveBePrefillInput): string | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeSalaireMensuelReference(input: MotifGraveBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: MotifGraveBePrefillInput): number {
  let count = 0;
  if (computeDateNotificationRupture(input) !== null) count++;
  if (computeSalaireMensuelReference(input) !== null) count++;
  return count;
}

export const MotifGraveBeSectionPrefillRules = {
  computeDateNotificationRupture,
  computeSalaireMensuelReference,
  computePrefillCount,
};
