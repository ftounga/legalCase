/**
 * F-236 SF-236-02 — Helper partagé `DevolutionLegalePrefillRules`.
 * 4 champs : conjointSurvivant (bool), nbDescendants (>= 0),
 * tousDescendantsCommunsAvecConjoint (bool), nbFreresSoeurs (>= 0).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

// SF-246-06 : les champs successions/libéralités sont désormais tous portés par le
// record backend `FamilleExtractedData` (chaîne `succession_detection` branchée) —
// plus de type d'intersection aspirationnel.
type Ai = Partial<FamilleExtractedData>;

export interface DevolutionLegalePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

const nullSafeBool = (v: unknown): boolean | null =>
  v === null || v === undefined ? null : Boolean(v);
const nonNegativeInt = (v: unknown): number | null =>
  typeof v === 'number' && v >= 0 ? v : null;

export function computeConjointSurvivant(input: DevolutionLegalePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.conjointSurvivantDetected);
}
export function computeNbDescendants(input: DevolutionLegalePrefillInput): number | null {
  return nonNegativeInt(input.aiData?.nbDescendantsDetecte);
}
export function computeTousDescendantsCommuns(input: DevolutionLegalePrefillInput): boolean | null {
  return nullSafeBool(input.aiData?.tousDescendantsCommunsAvecConjointDetected);
}
export function computeNbFreresSoeurs(input: DevolutionLegalePrefillInput): number | null {
  return nonNegativeInt(input.aiData?.nbFreresSoeursDetecte);
}

export function computePrefillCount(input: DevolutionLegalePrefillInput): number {
  let n = 0;
  if (computeConjointSurvivant(input) !== null) n++;
  if (computeNbDescendants(input) !== null) n++;
  if (computeTousDescendantsCommuns(input) !== null) n++;
  if (computeNbFreresSoeurs(input) !== null) n++;
  return n;
}

export const DevolutionLegalePrefillRules = {
  computeConjointSurvivant,
  computeNbDescendants,
  computeTousDescendantsCommuns,
  computeNbFreresSoeurs,
  computePrefillCount,
};
