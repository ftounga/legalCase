/**
 * F-236 SF-236-02 — Helper partagé `DivorceDesunionBePrefillRules`.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * SF-246-12 : remédiation dette D3 divorce-desunion-be (Famille BE).
 * - `computeDateSeparation` lit désormais `aiData.dateSeparationBe` (source backend
 *   réelle — `divorce_ddi_be_detection.date_separation_be`) au lieu de
 *   `aiData.dateSeparation` (champ FR aspirationnel — invariant §5.1.1).
 * - Validation ISO_DATE_RE strict (YYYY-MM-DD) — conformément au cadrage §5.1.6.
 * - `computeSeparationConsentue` reste no-op : `separationConsentue` est aspirationnel
 *   (aucune source backend BE ne l'extrait — hors périmètre SF-246-12).
 *
 * 1 champ réel branché : dateSeparation (← aiData.dateSeparationBe, BE uniquement).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Regex ISO date stricte YYYY-MM-DD (pas de date partielle). */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface DivorceDesunionBePrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: DivorceDesunionBePrefillInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

/**
 * SF-246-12 : lit `aiData.dateSeparationBe` (source backend réelle — BELGIQUE UNIQUEMENT).
 * Valide le format ISO YYYY-MM-DD strict — null sinon (fail-open, invariant §5.1.6).
 * DISTINCT de `dateSeparation` (FR — `vie_commune_detection` SF-246-08).
 */
export function computeDateSeparation(input: DivorceDesunionBePrefillInput): string | null {
  if (!isBelgium(input)) return null;
  const v = input.aiData?.dateSeparationBe;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/**
 * Aspirationnel — `separationConsentue` n'a pas de source backend BE en SF-246-12.
 * No-op gracieux : retourne toujours null.
 */
export function computeSeparationConsentue(_input: DivorceDesunionBePrefillInput): boolean | null {
  return null;
}

export function computePrefillCount(input: DivorceDesunionBePrefillInput): number {
  if (!isBelgium(input)) return 0;
  let n = 0;
  if (computeDateSeparation(input) !== null) n++;
  // computeSeparationConsentue toujours null — non compté.
  return n;
}

export const DivorceDesunionBePrefillRules = {
  computeDateSeparation,
  computeSeparationConsentue,
  computePrefillCount,
};
