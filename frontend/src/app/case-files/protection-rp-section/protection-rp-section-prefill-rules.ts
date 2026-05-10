/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil Protection RP.
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `ProtectionRpSectionComponent.prefillFromAi()` :
 *   `motifLicenciement ← mapMotifLicenciementFromIa(aiData.motifLicenciement)`.
 */

import {
  mapMotifLicenciementFromIa,
  MotifLicenciement,
} from '../../core/models/protection-rp.model';

export interface ProtectionRpPrefillInput {
  aiData?: { motifLicenciement?: string | null } | null;
}

export function computeMotifLicenciement(
  input: ProtectionRpPrefillInput,
): MotifLicenciement | null {
  const ai = input.aiData;
  if (!ai) return null;
  return mapMotifLicenciementFromIa(ai.motifLicenciement ?? null);
}

export function computePrefillCount(input: ProtectionRpPrefillInput): number {
  let count = 0;
  if (computeMotifLicenciement(input) !== null) count++;
  return count;
}

export const ProtectionRpSectionPrefillRules = {
  computeMotifLicenciement,
  computePrefillCount,
};
