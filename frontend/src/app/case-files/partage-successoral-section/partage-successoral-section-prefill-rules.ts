/**
 * F-236 SF-236-02 — Helper partagé `PartageSuccessoralPrefillRules`.
 * 3 champs : modePartageDemande, nombreCoheritiers (>= 2), dateDeces.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ModePartage } from '../../core/models/partage-successoral.model';

// SF-246-06 : les champs successions/libéralités sont désormais tous portés par le
// record backend `FamilleExtractedData` (chaîne `succession_detection` branchée) —
// plus de type d'intersection aspirationnel.
type Ai = Partial<FamilleExtractedData>;

export interface PartageSuccessoralPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseModeFromIa(value: unknown): ModePartage | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (!v) return null;
  if (v === 'PARTAGE_AMIABLE' || v === 'AMIABLE') return 'PARTAGE_AMIABLE';
  if (v === 'PARTAGE_JUDICIAIRE' || v === 'JUDICIAIRE') return 'PARTAGE_JUDICIAIRE';
  if (v === 'PARTAGE_PARTIEL' || v === 'PARTIEL') return 'PARTAGE_PARTIEL';
  return null;
}

export function computeModePartage(input: PartageSuccessoralPrefillInput): ModePartage | null {
  return parseModeFromIa(input.aiData?.modePartageDemandeDetecte);
}

export function computeNombreCoheritiers(input: PartageSuccessoralPrefillInput): number | null {
  const v = input.aiData?.nombreCoheritiersDetecte;
  return typeof v === 'number' && v >= 2 ? v : null;
}

export function computeDateDeces(input: PartageSuccessoralPrefillInput): string | null {
  const v = input.aiData?.dateDecesDetectee ?? input.aiData?.dateOuvertureSuccessionDetectee;
  return typeof v === 'string' && v.trim() ? v : null;
}

export function computePrefillCount(input: PartageSuccessoralPrefillInput): number {
  let n = 0;
  if (computeModePartage(input) !== null) n++;
  if (computeNombreCoheritiers(input) !== null) n++;
  if (computeDateDeces(input) !== null) n++;
  return n;
}

export const PartageSuccessoralPrefillRules = {
  parseModeFromIa,
  computeModePartage,
  computeNombreCoheritiers,
  computeDateDeces,
  computePrefillCount,
};
