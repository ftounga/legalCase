/**
 * F-236 SF-236-02 — Helper partagé `SeparationCorpsPrefillRules`.
 * 2 champs : dateJugementSeparationCorps (ISO via dateSeparation),
 * patrimoineCommun (boolean).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface SeparationCorpsPrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateJugement(input: SeparationCorpsPrefillInput): string | null {
  const v = input.aiData?.dateSeparation;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePatrimoineCommun(input: SeparationCorpsPrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommun;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: SeparationCorpsPrefillInput): number {
  let n = 0;
  if (computeDateJugement(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  return n;
}

export const SeparationCorpsPrefillRules = {
  ISO_DATE_REGEX,
  computeDateJugement,
  computePatrimoineCommun,
  computePrefillCount,
};
