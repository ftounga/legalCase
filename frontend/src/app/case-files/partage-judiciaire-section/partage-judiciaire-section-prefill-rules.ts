/**
 * F-236 SF-236-02 — Helper partagé `PartageJudiciairePrefillRules`.
 *
 * 4 champs : pvDifficultesEtabli (bool), tentativeAmiableEpuiseuee (bool),
 * nombreCoindivisaires (number >= 2), valeurEstimeeBiensEur (number >= 0).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData> & {
  pvDifficultesEtablisDetected?: boolean | null;
  tentativeAmiableEpuiseueeDetected?: boolean | null;
  nombreCoindivisairesDetecte?: number | null;
  valeurBiensIndivisionEur?: number | null;
};

export interface PartageJudiciairePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computePvDifficultes(input: PartageJudiciairePrefillInput): boolean | null {
  const v = input.aiData?.pvDifficultesEtablisDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeTentativeAmiable(input: PartageJudiciairePrefillInput): boolean | null {
  const v = input.aiData?.tentativeAmiableEpuiseueeDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeNombreCoindivisaires(input: PartageJudiciairePrefillInput): number | null {
  const v = input.aiData?.nombreCoindivisairesDetecte;
  return typeof v === 'number' && v >= 2 ? v : null;
}

export function computeValeurBiens(input: PartageJudiciairePrefillInput): number | null {
  const v = input.aiData?.valeurBiensIndivisionEur;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePrefillCount(input: PartageJudiciairePrefillInput): number {
  let n = 0;
  if (computePvDifficultes(input) !== null) n++;
  if (computeTentativeAmiable(input) !== null) n++;
  if (computeNombreCoindivisaires(input) !== null) n++;
  if (computeValeurBiens(input) !== null) n++;
  return n;
}

export const PartageJudiciairePrefillRules = {
  computePvDifficultes,
  computeTentativeAmiable,
  computeNombreCoindivisaires,
  computeValeurBiens,
  computePrefillCount,
};
