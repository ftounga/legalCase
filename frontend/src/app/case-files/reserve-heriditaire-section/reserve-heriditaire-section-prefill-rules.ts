/**
 * F-236 SF-236-02 — Helper partagé `ReserveHeriditairePrefillRules`.
 * 6 champs : nombreEnfants (>= 0, fallback nbDescendants), conjointSurvivant
 * (bool), montantSuccession (> 0), montantLibsTotal (>= 0),
 * dateOuvertureSuccession (string non vide), qualiteDuDemandeur (string non vide).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

// SF-246-06 : les champs successions/libéralités sont désormais tous portés par le
// record backend `FamilleExtractedData` (chaîne `succession_detection` branchée) —
// plus de type d'intersection aspirationnel.
type Ai = Partial<FamilleExtractedData>;

export interface ReserveHeriditairePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeNombreEnfants(input: ReserveHeriditairePrefillInput): number | null {
  const v = input.aiData?.nombreEnfantsSuccessionDetecte ?? input.aiData?.nbDescendantsDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}
export function computeConjointSurvivant(input: ReserveHeriditairePrefillInput): boolean | null {
  const v = input.aiData?.conjointSurvivantDetected;
  return v === null || v === undefined ? null : Boolean(v);
}
export function computeMontantSuccession(input: ReserveHeriditairePrefillInput): number | null {
  const v = input.aiData?.montantSuccessionEurDetecte;
  return typeof v === 'number' && v > 0 ? v : null;
}
export function computeMontantLibs(input: ReserveHeriditairePrefillInput): number | null {
  const v = input.aiData?.montantLibsTotalEurDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}
export function computeDateOuverture(input: ReserveHeriditairePrefillInput): string | null {
  const v = input.aiData?.dateOuvertureSuccessionDetectee;
  return typeof v === 'string' && v.length > 0 ? v : null;
}
export function computeQualiteDemandeur(input: ReserveHeriditairePrefillInput): string | null {
  const v = input.aiData?.qualiteDuDemandeurReserveDetecte;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: ReserveHeriditairePrefillInput): number {
  let n = 0;
  if (computeNombreEnfants(input) !== null) n++;
  if (computeConjointSurvivant(input) !== null) n++;
  if (computeMontantSuccession(input) !== null) n++;
  if (computeMontantLibs(input) !== null) n++;
  if (computeDateOuverture(input) !== null) n++;
  if (computeQualiteDemandeur(input) !== null) n++;
  return n;
}

export const ReserveHeriditairePrefillRules = {
  computeNombreEnfants,
  computeConjointSurvivant,
  computeMontantSuccession,
  computeMontantLibs,
  computeDateOuverture,
  computeQualiteDemandeur,
  computePrefillCount,
};
