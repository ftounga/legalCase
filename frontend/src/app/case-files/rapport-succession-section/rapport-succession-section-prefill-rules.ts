/**
 * F-236 SF-236-02 — Helper partagé `RapportSuccessionPrefillRules`.
 * 6 champs.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export type QualiteHeritierRapport = 'DESCENDANT' | 'CONJOINT_SURVIVANT';

type Ai = Partial<FamilleExtractedData> & {
  qualiteHeritierRapportDetectee?: string | null;
  montantDonationsRecuesEurDetecte?: number | null;
  valeurDonationAuJourPartageEurDetectee?: number | null;
  dateDonationDetectee?: string | null;
  donationDispenseDeRapportDetected?: boolean | null;
  naturePresumeeNonRapportableDetected?: boolean | null;
};

export interface RapportSuccessionPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeQualiteHeritier(input: RapportSuccessionPrefillInput): QualiteHeritierRapport | null {
  const v = input.aiData?.qualiteHeritierRapportDetectee;
  return v === 'DESCENDANT' || v === 'CONJOINT_SURVIVANT' ? v : null;
}

export function computeDonationsRecues(input: RapportSuccessionPrefillInput): number | null {
  const v = input.aiData?.montantDonationsRecuesEurDetecte;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeValeurPartage(input: RapportSuccessionPrefillInput): number | null {
  const v = input.aiData?.valeurDonationAuJourPartageEurDetectee;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateDonation(input: RapportSuccessionPrefillInput): string | null {
  const v = input.aiData?.dateDonationDetectee;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeDispense(input: RapportSuccessionPrefillInput): boolean | null {
  const v = input.aiData?.donationDispenseDeRapportDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeNatureNonRapportable(input: RapportSuccessionPrefillInput): boolean | null {
  const v = input.aiData?.naturePresumeeNonRapportableDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computePrefillCount(input: RapportSuccessionPrefillInput): number {
  let n = 0;
  if (computeQualiteHeritier(input) !== null) n++;
  if (computeDonationsRecues(input) !== null) n++;
  if (computeValeurPartage(input) !== null) n++;
  if (computeDateDonation(input) !== null) n++;
  if (computeDispense(input) !== null) n++;
  if (computeNatureNonRapportable(input) !== null) n++;
  return n;
}

export const RapportSuccessionPrefillRules = {
  computeQualiteHeritier,
  computeDonationsRecues,
  computeValeurPartage,
  computeDateDonation,
  computeDispense,
  computeNatureNonRapportable,
  computePrefillCount,
};
