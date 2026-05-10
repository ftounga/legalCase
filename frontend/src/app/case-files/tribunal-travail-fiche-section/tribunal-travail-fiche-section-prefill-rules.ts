/**
 * F-236 SF-236-02 — Helper partagé pour la fiche Tribunal du travail (BE).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `TribunalTravailFicheSectionComponent.prefillFromAi()` :
 *   `commissionParitaire ← aiData.conventionCollective`
 *   `typeContrat        ← normalize(aiData.typeContrat)` (EMPLOYE | OUVRIER)
 *   `dateDebut          ← aiData.dateEntree`
 *   `dateFin            ← aiData.dateLicenciement`
 *   `motifRupture       ← aiData.motifLicenciement`
 */

export interface TribunalTravailFichePrefillInput {
  aiData?: {
    conventionCollective?: string | null;
    typeContrat?: string | null;
    dateEntree?: string | null;
    dateLicenciement?: string | null;
    motifLicenciement?: string | null;
  } | null;
}

function nonEmpty(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeCommissionParitaire(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.conventionCollective);
}

export function computeTypeContrat(
  input: TribunalTravailFichePrefillInput,
): 'EMPLOYE' | 'OUVRIER' | null {
  const raw = input.aiData?.typeContrat;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const u = raw.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  if (u.includes('EMPLOYE') || u === 'CDI' || u === 'CDD') return 'EMPLOYE';
  if (u.includes('OUVRIER')) return 'OUVRIER';
  return null;
}

export function computeDateDebut(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.dateEntree);
}

export function computeDateFin(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.dateLicenciement);
}

export function computeMotifRupture(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.motifLicenciement);
}

export function computePrefillCount(input: TribunalTravailFichePrefillInput): number {
  let count = 0;
  if (computeCommissionParitaire(input) !== null) count++;
  if (computeTypeContrat(input) !== null) count++;
  if (computeDateDebut(input) !== null) count++;
  if (computeDateFin(input) !== null) count++;
  if (computeMotifRupture(input) !== null) count++;
  return count;
}

export const TribunalTravailFicheSectionPrefillRules = {
  computeCommissionParitaire,
  computeTypeContrat,
  computeDateDebut,
  computeDateFin,
  computeMotifRupture,
  computePrefillCount,
};
