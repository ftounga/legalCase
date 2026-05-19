/**
 * F-236 SF-236-02 — Helper partagé `RecherchePaternitePrefillRules`.
 * 6 champs.
 *
 * SF-246-09 : dateNaissanceEnfantRechercheDetectee est désormais un champ réel
 * de FamilleExtractedData (source backend `filiation_detection`) — le type
 * d'intersection aspirationnel est supprimé, lecture directe depuis le record.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData>;

export interface RecherchePaternitePrefillInput {
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

export function computeQualite(input: RecherchePaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.qualiteDuDemandeurRechercheDetected);
}
export function computeDateNaissance(input: RecherchePaternitePrefillInput): string | null {
  return nonEmptyString(input.aiData?.dateNaissanceEnfantRechercheDetectee);
}
export function computePresomptionPossessionEtat(input: RecherchePaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.presomptionPossessionEtatRechercheDetected);
}
export function computeExpertiseAdn(input: RecherchePaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.expertiseAdnDemandeeRechercheDetected);
}
export function computeRefusAdn(input: RecherchePaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.pereDesigneRefuseADNDetected);
}
export function computeMotifsSerieux(input: RecherchePaternitePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.motifsSerieuxRechercheDetected);
}

export function computePrefillCount(input: RecherchePaternitePrefillInput): number {
  let n = 0;
  if (computeQualite(input) !== null) n++;
  if (computeDateNaissance(input) !== null) n++;
  if (computePresomptionPossessionEtat(input) !== null) n++;
  if (computeExpertiseAdn(input) !== null) n++;
  if (computeRefusAdn(input) !== null) n++;
  if (computeMotifsSerieux(input) !== null) n++;
  return n;
}

export const RecherchePaternitePrefillRules = {
  computeQualite,
  computeDateNaissance,
  computePresomptionPossessionEtat,
  computeExpertiseAdn,
  computeRefusAdn,
  computeMotifsSerieux,
  computePrefillCount,
};
