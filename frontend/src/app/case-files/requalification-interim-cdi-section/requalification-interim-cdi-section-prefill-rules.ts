/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Requalification Intérim → CDI" (FR).
 * SF-246-21 : branchement des 5 champs date/durée/entreprise restants depuis `requalification_detection`.
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface RequalificationInterimCdiPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    // SF-246-21 — requalification_detection
    interimDureeTotaleMois?: number | null;
    interimDateFinDerniereMission?: string | null;
    interimNouvellesMissionDateDebut?: string | null;
    interimNouvellesMissionDateFin?: string | null;
    interimEntrepriseUtilisatrice?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: RequalificationInterimCdiPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : durée totale cumulée des missions en mois [0, 120]. */
export function computeDureeMissionsTotaleMois(input: RequalificationInterimCdiPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimDureeTotaleMois;
  return typeof v === 'number' && v >= 0 && v <= 120 ? v : null;
}

/** SF-246-21 : date de fin de la dernière mission d'intérim (ISO). */
export function computeDateFinDerniereMission(input: RequalificationInterimCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimDateFinDerniereMission;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de début d'une nouvelle mission (succession). */
export function computeNewMissionDateDebut(input: RequalificationInterimCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimNouvellesMissionDateDebut;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de fin d'une nouvelle mission. */
export function computeNewMissionDateFin(input: RequalificationInterimCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimNouvellesMissionDateFin;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : nom/SIRET de l'entreprise utilisatrice. */
export function computeNewMissionEntrepriseUtilisatrice(input: RequalificationInterimCdiPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimEntrepriseUtilisatrice;
  return typeof v === 'string' && v.trim().length > 0 ? v.trim() : null;
}

export function computePrefillCount(input: RequalificationInterimCdiPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDureeMissionsTotaleMois(input) !== null) count++;
  if (computeDateFinDerniereMission(input) !== null) count++;
  if (computeNewMissionDateDebut(input) !== null) count++;
  if (computeNewMissionDateFin(input) !== null) count++;
  if (computeNewMissionEntrepriseUtilisatrice(input) !== null) count++;
  return count;
}

export const RequalificationInterimCdiSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDureeMissionsTotaleMois,
  computeDateFinDerniereMission,
  computeNewMissionDateDebut,
  computeNewMissionDateFin,
  computeNewMissionEntrepriseUtilisatrice,
  computePrefillCount,
};
