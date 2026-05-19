/**
 * F-236 SF-236-02 — Helper partagé `CommunauteUniversellePrefillRules`.
 *
 * SF-246-07 : réalignement sur le record `FamilleExtractedData` réel (backend).
 * Suppression du type d'intersection aspirationnel : `valeurCommunauteEurDetectee`
 * est désormais un champ réel de `FamilleExtractedData` (exposé par le backend
 * via le sous-objet `regime_matrimonial_detection`).
 *
 * 4 champs : contratNotarie (bool), enfantsNonCommuns (bool),
 * clauseAttributionIntegrale (bool), valeurCommunauteEur (number > 0).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface CommunauteUniversellePrefillInput {
  aiData?: FamilleExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeContratNotarie(input: CommunauteUniversellePrefillInput): boolean | null {
  const v = input.aiData?.contratNotarieDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeEnfantsNonCommuns(input: CommunauteUniversellePrefillInput): boolean | null {
  const v = input.aiData?.enfantsNonCommunsDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeClauseAttributionIntegrale(
  input: CommunauteUniversellePrefillInput,
): boolean | null {
  const v = input.aiData?.clauseAttributionIntegraleDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

/**
 * SF-246-07 : valeur de la communauté (€) — champ réel `valeurCommunauteEurDetectee`.
 * Invariant §5.2 : montant strictement positif ou null (jamais 0).
 */
export function computeValeurCommunaute(input: CommunauteUniversellePrefillInput): number | null {
  const v = input.aiData?.valeurCommunauteEurDetectee;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: CommunauteUniversellePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
  let n = 0;
  if (computeContratNotarie(input) !== null) n++;
  if (computeEnfantsNonCommuns(input) !== null) n++;
  if (computeClauseAttributionIntegrale(input) !== null) n++;
  if (computeValeurCommunaute(input) !== null) n++;
  return n;
}

export const CommunauteUniversellePrefillRules = {
  computeContratNotarie,
  computeEnfantsNonCommuns,
  computeClauseAttributionIntegrale,
  computeValeurCommunaute,
  computePrefillCount,
};
