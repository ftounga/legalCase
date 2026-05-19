/**
 * F-236 SF-236-02 — Helper partagé `PartageJudiciairePrefillRules`.
 *
 * SF-246-07 : réalignement sur le record `FamilleExtractedData` réel (backend).
 * Suppression du type d'intersection aspirationnel : `nombreCoindivisairesDetecte`
 * et `valeurBiensIndivisionEur` sont désormais des champs réels de
 * `FamilleExtractedData` (exposés via le sous-objet `regime_matrimonial_detection`).
 *
 * 4 champs : pvDifficultesEtabli (bool), tentativeAmiableEpuiseuee (bool),
 * nombreCoindivisaires (number >= 2), valeurBiensIndivision (number > 0).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface PartageJudiciairePrefillInput {
  aiData?: FamilleExtractedData | null;
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

/**
 * SF-246-07 : nombre de coïndivisaires — champ réel `nombreCoindivisairesDetecte`.
 * Garde de plage : le partage judiciaire implique au moins 2 parties.
 */
export function computeNombreCoindivisaires(input: PartageJudiciairePrefillInput): number | null {
  const v = input.aiData?.nombreCoindivisairesDetecte;
  return typeof v === 'number' && v >= 2 ? v : null;
}

/**
 * SF-246-07 : valeur des biens en indivision (€) — champ réel `valeurBiensIndivisionEur`.
 * Invariant §5.2 : montant strictement positif ou null (jamais 0).
 */
export function computeValeurBiens(input: PartageJudiciairePrefillInput): number | null {
  const v = input.aiData?.valeurBiensIndivisionEur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: PartageJudiciairePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
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
