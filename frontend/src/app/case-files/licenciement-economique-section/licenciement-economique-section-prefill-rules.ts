/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Licenciement économique".
 * SF-246-21 : branchement de 2 champs depuis `rupture_collective_detection`
 * (âge salarié, ancienneté dérivée des champs existants).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `motifEconomiqueInvoque ← AI_MOTIF_TO_MOTIF_ECONOMIQUE[motifLicenciement.toUpperCase()]`
 *   `dateNotification       ← aiData.dateLicenciement (ISO YYYY-MM-DD)`
 *   `salarieAncienneteMois  ← dérivé dateEntree + dateLicenciement (existants)`
 *   `salarieAge             ← aiData.salarieAgeAnnees (SF-246-21)`
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
    dateEntree?: string | null;
    // SF-246-21 — rupture_collective_detection
    salarieAgeAnnees?: number | null;
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

/**
 * SF-246-21 : ancienneté en mois — dérivée de dateEntree + dateLicenciement (existants).
 * Null si dateEntree manquante ou invalide.
 */
export function computeSalarieAncienneteMois(input: LicenciementEconomiquePrefillInput): number | null {
  const dateEntree = input.aiData?.dateEntree;
  if (typeof dateEntree !== 'string' || !ISO_DATE_REGEX.test(dateEntree)) return null;
  const dateLic = input.aiData?.dateLicenciement;
  const endStr = (typeof dateLic === 'string' && ISO_DATE_REGEX.test(dateLic)) ? dateLic : null;
  const start = new Date(dateEntree);
  if (isNaN(start.getTime())) return null;
  const end = endStr ? new Date(endStr) : new Date();
  if (endStr && isNaN(end.getTime())) return null;
  if (end < start) return null;
  const years = end.getFullYear() - start.getFullYear();
  const months = end.getMonth() - start.getMonth();
  let total = years * 12 + months;
  if (end.getDate() < start.getDate()) total -= 1;
  return Math.max(total, 0);
}

/** SF-246-21 : âge du salarié en années [16, 80] depuis `rupture_collective_detection`. */
export function computeSalarieAge(input: LicenciementEconomiquePrefillInput): number | null {
  const v = input.aiData?.salarieAgeAnnees;
  return typeof v === 'number' && v >= 16 && v <= 80 ? v : null;
}

export function computePrefillCount(input: LicenciementEconomiquePrefillInput): number {
  let count = 0;
  if (computeMotifEconomique(input) !== null) count++;
  if (computeDateNotification(input) !== null) count++;
  if (computeSalarieAncienneteMois(input) !== null) count++;
  if (computeSalarieAge(input) !== null) count++;
  return count;
}

export const LicenciementEconomiqueSectionPrefillRules = {
  computeMotifEconomique,
  computeDateNotification,
  computeSalarieAncienneteMois,
  computeSalarieAge,
  computePrefillCount,
};
