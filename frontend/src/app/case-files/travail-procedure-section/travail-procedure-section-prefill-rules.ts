/**
 * F-237 SF-237-02 — Helper partagé `TravailProcedurePrefillRules` (module pur).
 *
 * Extrait la logique de pré-fill IA du composant `TravailProcedureSectionComponent`
 * (F-136 — Calendrier procédural travail FR + BE) selon le contrat F-236 SF-236-01.
 *
 * 2 champs pré-fillables :
 *   1. typeProcedure (code parmi `TRAVAIL_PROCEDURE_CODES`, gating pays via
 *      suffixe `_FR` / `_BE` vs `workspaceCountry`).
 *   2. dateDeclencheur (ISO YYYY-MM-DD).
 *
 * Le module est pur (aucun Angular import).
 */
import {
  TravailProcedureCode,
  TRAVAIL_PROCEDURE_CODES,
} from '../../core/models/travail-procedure.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

export const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Type d'aiData étendu utilisé par F-136 : les champs
 * `procedureTravailDetectee` et `dateDeclencheurProcedure` ne sont pas
 * encore déclarés sur `TravailExtractedData` côté modèle backend (anomalie
 * connue), d'où le cast permissif au runtime et ici.
 */
export type TravailProcedureAiData = TravailExtractedData & {
  procedureTravailDetectee?: string | null;
  dateDeclencheurProcedure?: string | null;
};

export interface TravailProcedurePrefillInput {
  aiData?: TravailProcedureAiData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/**
 * Code procédure travail mappé depuis `procedureTravailDetectee`, gating pays
 * via suffixe `_FR` / `_BE`.
 *
 * Retourne :
 *  - le code (TravailProcedureCode) si `detected` est une chaîne, listée dans
 *    `TRAVAIL_PROCEDURE_CODES` ET cohérente avec `workspaceCountry` ;
 *  - `null` sinon.
 *
 * Par défaut `workspaceCountry='FRANCE'` (miroir du runtime).
 */
export function computeTypeProcedure(
  input: TravailProcedurePrefillInput,
): TravailProcedureCode | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const detected = (input.aiData as any)?.procedureTravailDetectee;
  if (typeof detected !== 'string') return null;
  if (!TRAVAIL_PROCEDURE_CODES.has(detected as TravailProcedureCode)) return null;
  const country = input.workspaceCountry ?? 'FRANCE';
  const isFrance = country === 'FRANCE';
  const isFrCode = detected.endsWith('_FR');
  if ((isFrance && isFrCode) || (!isFrance && !isFrCode)) {
    return detected as TravailProcedureCode;
  }
  return null;
}

/**
 * Date déclencheur de la procédure — ISO YYYY-MM-DD strict.
 */
export function computeDateDeclencheur(
  input: TravailProcedurePrefillInput,
): string | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const v = (input.aiData as any)?.dateDeclencheurProcedure;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/**
 * Maître — compte les champs non-null. Strict miroir du runtime
 * `prefillFromAi()` de `TravailProcedureSectionComponent`.
 */
export function computePrefillCount(
  input: TravailProcedurePrefillInput,
): number {
  let n = 0;
  if (computeTypeProcedure(input) !== null) n++;
  if (computeDateDeclencheur(input) !== null) n++;
  return n;
}

export const TravailProcedurePrefillRules = {
  ISO_DATE_RE,
  computeTypeProcedure,
  computeDateDeclencheur,
  computePrefillCount,
};
