/**
 * F-236 SF-236-02 — Helper partagé `DivorceAlterationPrefillRules` (module pur).
 *
 * 5 champs pré-fillables depuis `aiData` (FamilleAiData) :
 *  - dateCessationVieCommune (string non vide)
 *  - dureeMariageAnnees (number >= 0)
 *  - revenusAnnuelsEpoux1Eur (number >= 0)
 *  - revenusAnnuelsEpoux2Eur (number >= 0)
 *  - patrimoineCommunSignificatif (boolean)
 */
import { FamilleAiData } from '../../core/models/divorce-alteration.model';

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

export function computePrefillCount(input: DivorceAlterationPrefillInput): number {
  let n = 0;
  if (computeDateCessation(input) !== null) n++;
  if (computeDureeMariage(input) !== null) n++;
  if (computeRevenusEpoux1(input) !== null) n++;
  if (computeRevenusEpoux2(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  return n;
}

export const DivorceAlterationPrefillRules = {
  computeDateCessation,
  computeDureeMariage,
  computeRevenusEpoux1,
  computeRevenusEpoux2,
  computePatrimoineCommun,
  computePrefillCount,
};
