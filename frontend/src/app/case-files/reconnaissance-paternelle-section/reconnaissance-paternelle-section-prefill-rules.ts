/**
 * F-236 SF-236-02 — Helper partagé `ReconnaissancePaternellePrefillRules`.
 * 5 champs : consentementLibreDuPere, paterniteVraisemblable,
 * enfantNonReconnuParAutrePere, procedureRespectee (booléens), dateNaissanceEnfant.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData> & {
  consentementLibreDuPereDetected?: boolean | null;
  paterniteVraisemblableDetected?: boolean | null;
  enfantNonReconnuParAutrePereDetected?: boolean | null;
  procedureRespecteeReconnaissanceDetected?: boolean | null;
  dateNaissanceEnfantDetectee?: string | null;
};

export interface ReconnaissancePaternellePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

const nullSafeBool = (v: unknown): boolean | null =>
  v === null || v === undefined ? null : Boolean(v);

export function computeConsentementLibre(input: ReconnaissancePaternellePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.consentementLibreDuPereDetected);
}
export function computePaterniteVraisemblable(input: ReconnaissancePaternellePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.paterniteVraisemblableDetected);
}
export function computeEnfantNonReconnu(input: ReconnaissancePaternellePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.enfantNonReconnuParAutrePereDetected);
}
export function computeProcedureRespectee(input: ReconnaissancePaternellePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.procedureRespecteeReconnaissanceDetected);
}
export function computeDateNaissance(input: ReconnaissancePaternellePrefillInput): string | null {
  const v = input.aiData?.dateNaissanceEnfantDetectee;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: ReconnaissancePaternellePrefillInput): number {
  let n = 0;
  if (computeConsentementLibre(input) !== null) n++;
  if (computePaterniteVraisemblable(input) !== null) n++;
  if (computeEnfantNonReconnu(input) !== null) n++;
  if (computeProcedureRespectee(input) !== null) n++;
  if (computeDateNaissance(input) !== null) n++;
  return n;
}

export const ReconnaissancePaternellePrefillRules = {
  computeConsentementLibre,
  computePaterniteVraisemblable,
  computeEnfantNonReconnu,
  computeProcedureRespectee,
  computeDateNaissance,
  computePrefillCount,
};
