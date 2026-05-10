/**
 * F-236 SF-236-02 — Helper partagé `DonationPrefillRules`.
 * 4 champs : formeDonation, dateDonation, saineDEsprit, respectQuotiteDisponible.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { FormeDonation } from '../../core/models/donation.model';

type Ai = Partial<FamilleExtractedData> & {
  formeDonationDetectee?: string | null;
  dateDonationDetectee?: string | null;
  saineDEspritDonateurDetected?: boolean | null;
  respectQuotiteDisponibleDetected?: boolean | null;
};

export interface DonationPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseFormeFromIa(value: unknown): FormeDonation | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (!v) return null;
  if (v === 'DONATION_NOTARIEE' || v === 'NOTARIEE' || v === 'NOTARIE') return 'DONATION_NOTARIEE';
  if (v === 'DONATION_MANUELLE' || v === 'MANUELLE' || v === 'MANUEL') return 'DONATION_MANUELLE';
  if (v === 'DON_INDIRECT' || v === 'INDIRECT' || v === 'INDIRECTE') return 'DON_INDIRECT';
  if (v === 'DONATION_DEGUISEE' || v === 'DEGUISEE' || v === 'DEGUISE') return 'DONATION_DEGUISEE';
  return null;
}

export function computeFormeDonation(input: DonationPrefillInput): FormeDonation | null {
  return parseFormeFromIa(input.aiData?.formeDonationDetectee);
}

export function computeDateDonation(input: DonationPrefillInput): string | null {
  const v = input.aiData?.dateDonationDetectee;
  return typeof v === 'string' && v.trim() ? v : null;
}

export function computeSaineDEsprit(input: DonationPrefillInput): boolean | null {
  const v = input.aiData?.saineDEspritDonateurDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeRespectQuotite(input: DonationPrefillInput): boolean | null {
  const v = input.aiData?.respectQuotiteDisponibleDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computePrefillCount(input: DonationPrefillInput): number {
  let n = 0;
  if (computeFormeDonation(input) !== null) n++;
  if (computeDateDonation(input) !== null) n++;
  if (computeSaineDEsprit(input) !== null) n++;
  if (computeRespectQuotite(input) !== null) n++;
  return n;
}

export const DonationPrefillRules = {
  parseFormeFromIa,
  computeFormeDonation,
  computeDateDonation,
  computeSaineDEsprit,
  computeRespectQuotite,
  computePrefillCount,
};
