/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Licenciement économique".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `motifEconomiqueInvoque ← AI_MOTIF_TO_MOTIF_ECONOMIQUE[motifLicenciement.toUpperCase()]`
 *   `dateNotification       ← aiData.dateLicenciement (ISO YYYY-MM-DD)`
 */

import {
  AI_MOTIF_TO_MOTIF_ECONOMIQUE,
  MotifEconomique,
} from '../../core/models/licenciement-economique.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface LicenciementEconomiquePrefillInput {
  aiData?: {
    motifLicenciement?: string | null;
    dateLicenciement?: string | null;
  } | null;
}

export function computeMotifEconomique(
  input: LicenciementEconomiquePrefillInput,
): MotifEconomique | null {
  const raw = input.aiData?.motifLicenciement;
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0) return null;
  const code = trimmed.toUpperCase();
  return AI_MOTIF_TO_MOTIF_ECONOMIQUE[code] ?? null;
}

export function computeDateNotification(input: LicenciementEconomiquePrefillInput): string | null {
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePrefillCount(input: LicenciementEconomiquePrefillInput): number {
  let count = 0;
  if (computeMotifEconomique(input) !== null) count++;
  if (computeDateNotification(input) !== null) count++;
  return count;
}

export const LicenciementEconomiqueSectionPrefillRules = {
  computeMotifEconomique,
  computeDateNotification,
  computePrefillCount,
};
