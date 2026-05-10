/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Inaptitude".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `salaireMensuelReference   ← aiData.salaireBrutMensuel > 0`
 *   `ancienneteAnnees          ← computeAncienneteAnnees(aiData.dateEntree)`
 *   `origineInaptitude         ← ORIGINE_IA_TO_FRONT[origineInaptitudePressentie]`
 *                                (skip si workspaceCountry === BELGIQUE)
 *   `avisMedecinTravailDate    ← aiData.avisMedecinTravailDate (ISO)`
 *   `reclassementRespecte      ← OUI → true, NON → false (INCONNU ignoré)`
 */

import { OrigineInaptitude } from '../../core/models/inaptitude.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const ORIGINE_IA_TO_FRONT: Record<string, OrigineInaptitude> = {
  ACCIDENT_TRAVAIL: 'PROFESSIONNELLE',
  MALADIE_PROFESSIONNELLE: 'PROFESSIONNELLE',
  MALADIE_ORDINAIRE: 'NON_PROFESSIONNELLE',
};

export interface InaptitudePrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    dateEntree?: string | null;
    origineInaptitudePressentie?: string | null;
    avisMedecinTravailDate?: string | null;
    reclassementRespecteDetected?: { reponse?: 'OUI' | 'NON' | 'INCONNU' } | null;
  } | null;
  workspaceCountry?: string;
  /** Date "now" injectable pour les tests — défaut Date.now(). */
  now?: Date;
}

export function computeSalaireMensuel(input: InaptitudePrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeAncienneteAnnees(input: InaptitudePrefillInput): number | null {
  const dateEntree = input.aiData?.dateEntree;
  if (typeof dateEntree !== 'string' || !ISO_DATE_REGEX.test(dateEntree)) return null;
  const entree = new Date(dateEntree);
  if (isNaN(entree.getTime())) return null;
  const now = input.now ?? new Date();
  if (entree > now) return null;
  const diffMs = now.getTime() - entree.getTime();
  const years = Math.floor(diffMs / (365.25 * 24 * 60 * 60 * 1000));
  return years >= 0 ? years : null;
}

export function computeOrigineInaptitude(input: InaptitudePrefillInput): OrigineInaptitude | null {
  if (input.workspaceCountry === 'BELGIQUE') return null;
  const raw = input.aiData?.origineInaptitudePressentie;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  return ORIGINE_IA_TO_FRONT[raw] ?? null;
}

export function computeAvisMedecinDate(input: InaptitudePrefillInput): string | null {
  const v = input.aiData?.avisMedecinTravailDate;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computeReclassementRespecte(input: InaptitudePrefillInput): boolean | null {
  const r = input.aiData?.reclassementRespecteDetected?.reponse;
  if (r === 'OUI') return true;
  if (r === 'NON') return false;
  return null; // INCONNU / undefined ne pré-remplit pas.
}

export function computePrefillCount(input: InaptitudePrefillInput): number {
  let count = 0;
  if (computeSalaireMensuel(input) !== null) count++;
  if (computeAncienneteAnnees(input) !== null) count++;
  if (computeOrigineInaptitude(input) !== null) count++;
  if (computeAvisMedecinDate(input) !== null) count++;
  if (computeReclassementRespecte(input) !== null) count++;
  return count;
}

export const InaptitudeSectionPrefillRules = {
  computeSalaireMensuel,
  computeAncienneteAnnees,
  computeOrigineInaptitude,
  computeAvisMedecinDate,
  computeReclassementRespecte,
  computePrefillCount,
  ORIGINE_IA_TO_FRONT,
};
