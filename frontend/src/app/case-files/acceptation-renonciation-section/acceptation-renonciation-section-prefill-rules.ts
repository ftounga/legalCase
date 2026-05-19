/**
 * F-236 SF-236-02 — Helper partagé `AcceptationRenonciationPrefillRules`.
 * 6 champs : dateOuvertureSuccession (ISO), actifBrut (>= 0), passif (>= 0),
 * qualiteHeritier ('PREMIER_RANG'|'SECOND_RANG'),
 * actesEquivalentAcceptation (bool), dettesIncertaines (bool).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
export type QualiteHeritierRang = 'PREMIER_RANG' | 'SECOND_RANG';

// SF-246-06 : les champs successions/libéralités sont désormais tous portés par le
// record backend `FamilleExtractedData` (chaîne `succession_detection` branchée) —
// plus de type d'intersection aspirationnel.
type Ai = Partial<FamilleExtractedData>;

export interface AcceptationRenonciationPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateOuverture(input: AcceptationRenonciationPrefillInput): string | null {
  const v = input.aiData?.dateOuvertureSuccessionDetectee;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computeActifBrut(input: AcceptationRenonciationPrefillInput): number | null {
  const v = input.aiData?.actifBrutSuccessionEurDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePassif(input: AcceptationRenonciationPrefillInput): number | null {
  const v = input.aiData?.passifSuccessionEurDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computeQualiteHeritier(input: AcceptationRenonciationPrefillInput): QualiteHeritierRang | null {
  const v = input.aiData?.qualiteHeritierDetectee;
  return v === 'PREMIER_RANG' || v === 'SECOND_RANG' ? v : null;
}

export function computeActesEquivalentAcceptation(input: AcceptationRenonciationPrefillInput): boolean | null {
  const v = input.aiData?.actesEquivalentAcceptationDejaPosesDetected;
  return typeof v === 'boolean' ? v : null;
}

export function computeDettesIncertaines(input: AcceptationRenonciationPrefillInput): boolean | null {
  const v = input.aiData?.dettesIncertainesDetected;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: AcceptationRenonciationPrefillInput): number {
  let n = 0;
  if (computeDateOuverture(input) !== null) n++;
  if (computeActifBrut(input) !== null) n++;
  if (computePassif(input) !== null) n++;
  if (computeQualiteHeritier(input) !== null) n++;
  if (computeActesEquivalentAcceptation(input) !== null) n++;
  if (computeDettesIncertaines(input) !== null) n++;
  return n;
}

export const AcceptationRenonciationPrefillRules = {
  ISO_DATE_REGEX,
  computeDateOuverture,
  computeActifBrut,
  computePassif,
  computeQualiteHeritier,
  computeActesEquivalentAcceptation,
  computeDettesIncertaines,
  computePrefillCount,
};
