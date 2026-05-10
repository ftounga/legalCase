/**
 * F-236 SF-236-02 — Helper partagé `RevisionsPostDivorcePrefillRules`.
 *
 * 3 champs : revenusActuelsDebiteurEur, revenusActuelsCreancierEur, nbEnfantsACharge.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface RevisionsPostDivorcePrefillInput {
  aiData?: (Partial<FamilleExtractedData> & { nbEnfantsACharge?: number | null }) | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeRevenusDebiteur(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux1Eur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeRevenusCreancier(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux2Eur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeNbEnfants(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.nbEnfantsACharge;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePrefillCount(input: RevisionsPostDivorcePrefillInput): number {
  let n = 0;
  if (computeRevenusDebiteur(input) !== null) n++;
  if (computeRevenusCreancier(input) !== null) n++;
  if (computeNbEnfants(input) !== null) n++;
  return n;
}

export const RevisionsPostDivorcePrefillRules = {
  computeRevenusDebiteur,
  computeRevenusCreancier,
  computeNbEnfants,
  computePrefillCount,
};
