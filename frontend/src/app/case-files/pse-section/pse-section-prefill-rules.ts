/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil PSE.
 * SF-246-21 : branchement de 2 champs depuis `rupture_collective_detection`
 * (nombre salariés, nombre licenciements).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `PseSectionComponent.prefillFromAi()` :
 *   `dateProjet              ← aiData.dateLicenciement` (1ʳᵉ approximation).
 *   `tailleEntrepriseSalaries ← aiData.pseNombreSalaries` (SF-246-21).
 *   `nombreLicenciements     ← aiData.pseNombreLicenciements` (SF-246-21).
 */

export interface PsePrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    // SF-246-21 — rupture_collective_detection
    pseNombreSalaries?: number | null;
    pseNombreLicenciements?: number | null;
  } | null;
}

export function computeDateProjet(input: PsePrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const date = ai.dateLicenciement;
  if (typeof date !== 'string' || date.length === 0) return null;
  return date;
}

/** SF-246-21 : effectif de l'entreprise en nb salariés [0, 100000]. */
export function computeTailleEntrepriseSalaries(input: PsePrefillInput): number | null {
  const v = input.aiData?.pseNombreSalaries;
  return typeof v === 'number' && v >= 0 && v <= 100_000 ? v : null;
}

/** SF-246-21 : nombre de licenciements envisagés dans le PSE [0, 100000]. */
export function computeNombreLicenciements(input: PsePrefillInput): number | null {
  const v = input.aiData?.pseNombreLicenciements;
  return typeof v === 'number' && v >= 0 && v <= 100_000 ? v : null;
}

export function computePrefillCount(input: PsePrefillInput): number {
  let count = 0;
  if (computeDateProjet(input) !== null) count++;
  if (computeTailleEntrepriseSalaries(input) !== null) count++;
  if (computeNombreLicenciements(input) !== null) count++;
  return count;
}

export const PseSectionPrefillRules = {
  computeDateProjet,
  computeTailleEntrepriseSalaries,
  computeNombreLicenciements,
  computePrefillCount,
};
