/**
 * F-236 SF-236-02 — Helper partagé `MediationFamilialePrefillRules`.
 * 1 champ : motifSaisine (string non vide). Refactor du static existant.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { MediationMotifSaisine } from '../../core/models/mediation-familiale.model';

type Ai = Partial<FamilleExtractedData> & {
  motifSaisineMediationDetecte?: string | null;
};

export interface MediationFamilialePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeMotifSaisine(input: MediationFamilialePrefillInput): MediationMotifSaisine | null {
  const v = input.aiData?.motifSaisineMediationDetecte;
  return typeof v === 'string' && v.length > 0 ? (v as MediationMotifSaisine) : null;
}

export function computePrefillCount(input: MediationFamilialePrefillInput): number {
  return computeMotifSaisine(input) !== null ? 1 : 0;
}

export const MediationFamilialePrefillRules = {
  computeMotifSaisine,
  computePrefillCount,
};
