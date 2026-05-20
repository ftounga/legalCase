/**
 * F-236 SF-236-02 — Helper partagé `DivorceAcceptePrefillRules` (module pur).
 *
 * SF-246-27 : 6 champs pré-fillables :
 *   1. dureeMariageAnnees (int >= 0)
 *   2. revenusAnnuelsEpoux1Eur (number >= 0)
 *   3. revenusAnnuelsEpoux2Eur (number >= 0)
 *   4. patrimoineCommun (boolean)
 *   5. dateAcceptationPV (ISO YYYY-MM-DD)
 *   6. dateAssignation (ISO YYYY-MM-DD) — SF-246-27 via `dateAssignationDivorce`.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface DivorceAcceptePrefillInput {
  aiData?: FamilleExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDureeMariage(input: DivorceAcceptePrefillInput): number | null {
  const v = input.aiData?.dureeMariageAnnees;
  return typeof v === 'number' && v >= 0 && Number.isInteger(v) ? v : null;
}

export function computeRevenusEpoux1(input: DivorceAcceptePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux1Eur;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computeRevenusEpoux2(input: DivorceAcceptePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux2Eur;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePatrimoineCommun(input: DivorceAcceptePrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommun;
  return typeof v === 'boolean' ? v : null;
}

export function computeDateAcceptationPV(input: DivorceAcceptePrefillInput): string | null {
  const v = input.aiData?.dateAcceptationPV;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/**
 * SF-246-27 : date de l'assignation en divorce (art. 56 CPC), ISO YYYY-MM-DD.
 * Source backend réelle : `protection_divorce_detection_v2.date_assignation_divorce`.
 */
export function computeDateAssignation(input: DivorceAcceptePrefillInput): string | null {
  const v = input.aiData?.dateAssignationDivorce;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePrefillCount(input: DivorceAcceptePrefillInput): number {
  let n = 0;
  if (computeDureeMariage(input) !== null) n++;
  if (computeRevenusEpoux1(input) !== null) n++;
  if (computeRevenusEpoux2(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  if (computeDateAcceptationPV(input) !== null) n++;
  if (computeDateAssignation(input) !== null) n++;
  return n;
}

export const DivorceAcceptePrefillRules = {
  ISO_DATE_REGEX,
  computeDureeMariage,
  computeRevenusEpoux1,
  computeRevenusEpoux2,
  computePatrimoineCommun,
  computeDateAcceptationPV,
  computeDateAssignation,
  computePrefillCount,
};
