/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Fin de mission intérim" (FR).
 * SF-246-21 : branchement de 3 champs depuis `requalification_detection`
 * (total rémunérations, durée mission jours, date fin mission).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface FinMissionInterimPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    // SF-246-21 — requalification_detection
    interimTotalRemunerationsBrutes?: number | null;
    interimDureeMissionJours?: number | null;
    interimDateFinDerniereMission?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelReference(input: FinMissionInterimPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : total des rémunérations brutes sur toutes missions (€ > 0). */
export function computeTotalRemunerationsBrutes(input: FinMissionInterimPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimTotalRemunerationsBrutes;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : durée de la mission en jours calendaires [0, 3650]. */
export function computeDureeMissionJours(input: FinMissionInterimPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimDureeMissionJours;
  return typeof v === 'number' && v >= 0 && v <= 3650 ? v : null;
}

/** SF-246-21 : date de fin de la dernière mission d'intérim (ISO). */
export function computeDateFinMission(input: FinMissionInterimPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.interimDateFinDerniereMission;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

export function computePrefillCount(input: FinMissionInterimPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  if (computeTotalRemunerationsBrutes(input) !== null) count++;
  if (computeDureeMissionJours(input) !== null) count++;
  if (computeDateFinMission(input) !== null) count++;
  return count;
}

export const FinMissionInterimSectionPrefillRules = {
  computeSalaireMensuelReference,
  computeTotalRemunerationsBrutes,
  computeDureeMissionJours,
  computeDateFinMission,
  computePrefillCount,
};
