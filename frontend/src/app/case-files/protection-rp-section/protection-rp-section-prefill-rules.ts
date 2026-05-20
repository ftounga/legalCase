/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil Protection RP.
 * SF-246-21 : branchement de 2 champs supplémentaires depuis `aiData` :
 *   `datePresumeeRupture ← aiData.dateLicenciement` (ISO, déjà extraite).
 *   `salaireMensuelBrut  ← aiData.salaireBrutMensuel` (€ > 0, déjà extrait).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `ProtectionRpSectionComponent.prefillFromAi()` :
 *   `motifLicenciement ← mapMotifLicenciementFromIa(aiData.motifLicenciement)`.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

import {
  mapMotifLicenciementFromIa,
  MotifLicenciement,
} from '../../core/models/protection-rp.model';

export interface ProtectionRpPrefillInput {
  aiData?: {
    motifLicenciement?: string | null;
    // SF-246-21 — champs existants réutilisés pour pré-fill
    dateLicenciement?: string | null;
    salaireBrutMensuel?: number | null;
  } | null;
}

export function computeMotifLicenciement(
  input: ProtectionRpPrefillInput,
): MotifLicenciement | null {
  const ai = input.aiData;
  if (!ai) return null;
  return mapMotifLicenciementFromIa(ai.motifLicenciement ?? null);
}

/**
 * SF-246-21 : date présumée de la rupture ← aiData.dateLicenciement (ISO).
 * Accepte uniquement les dates ISO strictes.
 */
export function computeDatePresumeeRupture(input: ProtectionRpPrefillInput): string | null {
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : salaire mensuel brut ← aiData.salaireBrutMensuel (€ > 0). */
export function computeSalaireMensuelBrut(input: ProtectionRpPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: ProtectionRpPrefillInput): number {
  let count = 0;
  if (computeMotifLicenciement(input) !== null) count++;
  if (computeDatePresumeeRupture(input) !== null) count++;
  if (computeSalaireMensuelBrut(input) !== null) count++;
  return count;
}

export const ProtectionRpSectionPrefillRules = {
  computeMotifLicenciement,
  computeDatePresumeeRupture,
  computeSalaireMensuelBrut,
  computePrefillCount,
};
