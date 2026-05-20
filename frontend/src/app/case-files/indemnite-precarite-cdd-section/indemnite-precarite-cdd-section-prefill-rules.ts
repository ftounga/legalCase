/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Indemnité précarité CDD" (FR).
 * SF-246-21 : branchement de 2 champs depuis `requalification_detection` (cdd_duree_mois,
 * cdd_total_salaires_bruts).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface IndemnitePrecariteCddPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    // SF-246-21 — requalification_detection
    cddDureeMois?: number | null;
    cddTotalSalairesBruts?: number | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelReference(input: IndemnitePrecariteCddPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : durée du CDD en mois [0, 120] — mutualisé depuis requalification_detection. */
export function computeDureeCddMois(input: IndemnitePrecariteCddPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddDureeMois;
  return typeof v === 'number' && v >= 0 && v <= 120 ? v : null;
}

/** SF-246-21 : total des salaires bruts sur la durée du CDD (€ > 0). */
export function computeTotalSalairesBruts(input: IndemnitePrecariteCddPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.cddTotalSalairesBruts;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: IndemnitePrecariteCddPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  if (computeDureeCddMois(input) !== null) count++;
  if (computeTotalSalairesBruts(input) !== null) count++;
  return count;
}

export const IndemnitePrecariteCddSectionPrefillRules = {
  computeSalaireMensuelReference,
  computeDureeCddMois,
  computeTotalSalairesBruts,
  computePrefillCount,
};
