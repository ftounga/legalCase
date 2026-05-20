/**
 * F-236 SF-236-02 — Helper partagé `DivorceAlterationPrefillRules` (module pur).
 *
 * SF-246-27 : 6 champs pré-fillables depuis `aiData` (FamilleAiData) :
 *  - dateCessationVieCommune (string non vide)
 *  - dureeMariageAnnees (number >= 0)
 *  - revenusAnnuelsEpoux1Eur (number >= 0)
 *  - revenusAnnuelsEpoux2Eur (number >= 0)
 *  - patrimoineCommunSignificatif (boolean)
 *  - dateAssignationDivorce (ISO YYYY-MM-DD) — SF-246-27 source réelle.
 */
import { FamilleAiData } from '../../core/models/divorce-alteration.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface DivorceAlterationPrefillInput {
  aiData?: FamilleAiData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateCessation(input: DivorceAlterationPrefillInput): string | null {
  const v = input.aiData?.dateCessationVieCommune;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeDureeMariage(input: DivorceAlterationPrefillInput): number | null {
  const v = input.aiData?.dureeMariageAnnees;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computeRevenusEpoux1(input: DivorceAlterationPrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux1Eur;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computeRevenusEpoux2(input: DivorceAlterationPrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux2Eur;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePatrimoineCommun(input: DivorceAlterationPrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommunSignificatif;
  return typeof v === 'boolean' ? v : null;
}

/**
 * SF-246-27 : date de l'assignation en divorce pour altération (art. 56 CPC),
 * ISO YYYY-MM-DD. Source backend réelle :
 * `protection_divorce_detection_v2.date_assignation_divorce`.
 */
export function computeDateAssignation(input: DivorceAlterationPrefillInput): string | null {
  const v = input.aiData?.dateAssignationDivorce;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePrefillCount(input: DivorceAlterationPrefillInput): number {
  let n = 0;
  if (computeDateCessation(input) !== null) n++;
  if (computeDureeMariage(input) !== null) n++;
  if (computeRevenusEpoux1(input) !== null) n++;
  if (computeRevenusEpoux2(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  if (computeDateAssignation(input) !== null) n++;
  return n;
}

export const DivorceAlterationPrefillRules = {
  ISO_DATE_REGEX,
  computeDateCessation,
  computeDureeMariage,
  computeRevenusEpoux1,
  computeRevenusEpoux2,
  computePatrimoineCommun,
  computeDateAssignation,
  computePrefillCount,
};
