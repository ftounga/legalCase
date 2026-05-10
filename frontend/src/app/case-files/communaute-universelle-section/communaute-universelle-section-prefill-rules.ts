/**
 * F-236 SF-236-02 — Helper partagé `CommunauteUniversellePrefillRules`.
 *
 * 4 champs : contratNotarie (bool), enfantsNonCommuns (bool),
 * clauseAttributionIntegrale (bool), valeurCommunauteEur (number >= 0).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData> & {
  contratNotarieDetected?: boolean | null;
  enfantsNonCommunsDetected?: boolean | null;
  clauseAttributionIntegraleDetected?: boolean | null;
  valeurCommunauteEurDetectee?: number | null;
};

export interface CommunauteUniversellePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeContratNotarie(input: CommunauteUniversellePrefillInput): boolean | null {
  const v = input.aiData?.contratNotarieDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeEnfantsNonCommuns(input: CommunauteUniversellePrefillInput): boolean | null {
  const v = input.aiData?.enfantsNonCommunsDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeClauseAttributionIntegrale(
  input: CommunauteUniversellePrefillInput,
): boolean | null {
  const v = input.aiData?.clauseAttributionIntegraleDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeValeurCommunaute(input: CommunauteUniversellePrefillInput): number | null {
  const v = input.aiData?.valeurCommunauteEurDetectee;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePrefillCount(input: CommunauteUniversellePrefillInput): number {
  let n = 0;
  if (computeContratNotarie(input) !== null) n++;
  if (computeEnfantsNonCommuns(input) !== null) n++;
  if (computeClauseAttributionIntegrale(input) !== null) n++;
  if (computeValeurCommunaute(input) !== null) n++;
  return n;
}

export const CommunauteUniversellePrefillRules = {
  computeContratNotarie,
  computeEnfantsNonCommuns,
  computeClauseAttributionIntegrale,
  computeValeurCommunaute,
  computePrefillCount,
};
