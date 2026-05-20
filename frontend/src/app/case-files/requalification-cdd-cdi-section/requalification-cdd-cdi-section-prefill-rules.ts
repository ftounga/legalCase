/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Requalification CDD → CDI" (FR).
 * SF-246-21 : branchement des 4 champs date/durée restants depuis `requalification_detection`.
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface RequalificationCddCdiPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    // SF-246-21 — requalification_detection
    cddDureeMois?: number | null;
    cddDateFinDernierContrat?: string | null;
    cddNouveauDateDebut?: string | null;
    cddNouveauDateFin?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: RequalificationCddCdiPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : durée du dernier CDD en mois [0, 120]. */
export function computeDureeCddMois(input: RequalificationCddCdiPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddDureeMois;
  return typeof v === 'number' && v >= 0 && v <= 120 ? v : null;
}

/** SF-246-21 : date de fin du dernier CDD (ISO YYYY-MM-DD). */
export function computeDateFinDernierContrat(input: RequalificationCddCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddDateFinDernierContrat;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de début du CDD suivant (succession). */
export function computeNewCddDateDebut(input: RequalificationCddCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddNouveauDateDebut;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de fin du CDD suivant. */
export function computeNewCddDateFin(input: RequalificationCddCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddNouveauDateFin;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

export function computePrefillCount(input: RequalificationCddCdiPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDureeCddMois(input) !== null) count++;
  if (computeDateFinDernierContrat(input) !== null) count++;
  if (computeNewCddDateDebut(input) !== null) count++;
  if (computeNewCddDateFin(input) !== null) count++;
  return count;
}

export const RequalificationCddCdiSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDureeCddMois,
  computeDateFinDernierContrat,
  computeNewCddDateDebut,
  computeNewCddDateFin,
  computePrefillCount,
};
