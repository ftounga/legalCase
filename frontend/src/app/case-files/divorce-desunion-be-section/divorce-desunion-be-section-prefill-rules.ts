/**
 * F-236 SF-236-02 — Helper partagé `DivorceDesunionBePrefillRules`.
 *
 * 2 champs : dateSeparation (string non vide), separationConsentue (boolean).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface DivorceDesunionBePrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateSeparation(input: DivorceDesunionBePrefillInput): string | null {
  const v = input.aiData?.dateSeparation;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeSeparationConsentue(input: DivorceDesunionBePrefillInput): boolean | null {
  const v = input.aiData?.separationConsentue;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: DivorceDesunionBePrefillInput): number {
  let n = 0;
  if (computeDateSeparation(input) !== null) n++;
  if (computeSeparationConsentue(input) !== null) n++;
  return n;
}

export const DivorceDesunionBePrefillRules = {
  computeDateSeparation,
  computeSeparationConsentue,
  computePrefillCount,
};
