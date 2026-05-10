/**
 * F-236 SF-236-02 — Helper partagé `ContestationPaternitePrefillRules`.
 * 6 champs : qualiteAagir (string), dateEtablissementFiliation (string),
 * dateConnaissanceVerite (string), dateMajoriteEnfant (string),
 * possessionEtatConforme5Ans (boolean), expertiseAdnDemandee (boolean — runtime
 * gate sur "!this.expertiseAdnDemandee" donc compté quand defined).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData> & {
  qualiteAagirContestationDetected?: string | null;
  dateEtablissementFiliationDetectee?: string | null;
  dateConnaissanceVeriteDetectee?: string | null;
  dateMajoriteEnfantDetectee?: string | null;
  possessionEtatConforme5AnsDetected?: boolean | null;
  expertiseAdnDemandeeDetected?: boolean | null;
  motifsSerieuxDetected?: boolean | null;
};

export interface ContestationPaternitePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

const nonEmptyString = (v: unknown): string | null =>
  typeof v === 'string' && v.length > 0 ? v : null;
const nullSafeBool = (v: unknown): boolean | null =>
  v === null || v === undefined ? null : Boolean(v);

export function computeQualiteAagir(input: ContestationPaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.qualiteAagirContestationDetected);
}
export function computeDateEtablissement(input: ContestationPaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.dateEtablissementFiliationDetectee);
}
export function computeDateConnaissance(input: ContestationPaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.dateConnaissanceVeriteDetectee);
}
export function computeDateMajorite(input: ContestationPaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.dateMajoriteEnfantDetectee);
}
export function computePossessionEtat(input: ContestationPaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.possessionEtatConforme5AnsDetected);
}
export function computeExpertiseAdn(input: ContestationPaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.expertiseAdnDemandeeDetected);
}
export function computeMotifsSerieux(input: ContestationPaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.motifsSerieuxDetected);
}

export function computePrefillCount(input: ContestationPaternitePrefillInput): number {
  let n = 0;
  if (computeQualiteAagir(input) !== null) n++;
  if (computeDateEtablissement(input) !== null) n++;
  if (computeDateConnaissance(input) !== null) n++;
  if (computeDateMajorite(input) !== null) n++;
  if (computePossessionEtat(input) !== null) n++;
  if (computeExpertiseAdn(input) !== null) n++;
  if (computeMotifsSerieux(input) !== null) n++;
  return n;
}

export const ContestationPaternitePrefillRules = {
  computeQualiteAagir,
  computeDateEtablissement,
  computeDateConnaissance,
  computeDateMajorite,
  computePossessionEtat,
  computeExpertiseAdn,
  computeMotifsSerieux,
  computePrefillCount,
};
