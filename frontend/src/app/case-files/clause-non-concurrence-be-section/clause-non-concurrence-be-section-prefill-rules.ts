import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ClauseNonConcurrenceBeZone } from '../../core/models/clause-non-concurrence-be.model';

/**
 * SF-213-01b — Helper partagé pour le pré-remplissage IA de
 * `ClauseNonConcurrenceBeSectionComponent` (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 3 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `remunerationAnnuelleBrute` ← `aiData.salaireBrutAnnuel`
 *      (number > 0 strict).
 *   2. `dureeMois`                 ← `aiData.clauseNonConcurrenceDureeMois`
 *      (entier dans [1, 12]).
 *   3. `zoneGeographique`          ← `aiData.clauseNonConcurrenceZone`
 *      (mapping string → enum strict ; whitelist 3 valeurs).
 *
 * `activiteInternationaleProuvee` n'est volontairement pas instrumenté —
 * c'est un jugement de l'avocat sur la preuve effective de l'activité
 * internationale (factualisation insuffisante depuis le seul contrat).
 * Reste toujours `false` par défaut.
 *
 * Gating : `workspaceCountry !== 'BELGIQUE'` → null (outil mono-pays BE).
 */

const ZONE_WHITELIST: ReadonlySet<ClauseNonConcurrenceBeZone> = new Set([
  'BELGIQUE_UNIQUEMENT',
  'BELGIQUE_ET_ETRANGER',
  'NON_SPECIFIEE',
]);

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value <= 0) return null;
  return value;
}

function intInRangeOrNull(value: unknown, min: number, max: number): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (!Number.isInteger(value)) return null;
  if (value < min || value > max) return null;
  return value;
}

export const ClauseNonConcurrenceBeSectionPrefillRules = {

  /**
   * Rémunération annuelle brute : nombre > 0. Toute valeur ≤ 0, NaN, ou
   * non-number → null. Parité backend `@DecimalMin(value="0.01", inclusive=true)`.
   */
  computeRemunerationAnnuelleBrute(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull((ai as { salaireBrutAnnuel?: unknown }).salaireBrutAnnuel);
  },

  /**
   * Durée de la clause en mois : entier 1-12 (limite légale Loi 03/07/1978).
   * Toute valeur hors borne, non-entière, ou non-number → null.
   */
  computeDureeMois(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return intInRangeOrNull(
      (ai as { clauseNonConcurrenceDureeMois?: unknown }).clauseNonConcurrenceDureeMois,
      1,
      12,
    );
  },

  /**
   * Zone géographique : mapping string → enum strict. Toute valeur hors
   * whitelist (vide, casse incorrecte, libellé inconnu) → null (l'avocat
   * tranche). Trim appliqué avant comparaison.
   */
  computeZoneGeographique(input: PrefillCountInput): ClauseNonConcurrenceBeZone | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { clauseNonConcurrenceZone?: unknown }).clauseNonConcurrenceZone;
    if (typeof raw !== 'string') return null;
    const candidate = raw.trim() as ClauseNonConcurrenceBeZone;
    return ZONE_WHITELIST.has(candidate) ? candidate : null;
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 3.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRemunerationAnnuelleBrute(input) !== null) n++;
    if (this.computeDureeMois(input) !== null) n++;
    if (this.computeZoneGeographique(input) !== null) n++;
    return n;
  },
} as const;
