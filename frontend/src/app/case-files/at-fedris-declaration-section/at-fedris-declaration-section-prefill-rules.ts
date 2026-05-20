import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-04b — Helper partagé pour le pré-remplissage IA de
 * `AtFedrisDeclarationSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 2 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `dateAccident` ← `aiData.dateAccident`
 *   2. `dateConnaissanceEmployeur` ← `aiData.dateConnaissanceAccidentEmployeur`
 *
 * `dateActionEnvisagee` et `dateDeclarationEffectuee` ne sont pas pré-remplis
 * (informations circonstancielles saisies par l'avocat).
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE.test(trimmed)) return null;
  const parsed = Date.parse(trimmed);
  if (Number.isNaN(parsed)) return null;
  return trimmed;
}

export const AtFedrisDeclarationPrefillRules = {

  computeDateAccident(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateAccident);
  },

  computeDateConnaissanceEmployeur(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateConnaissanceAccidentEmployeur);
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec). Max = 2.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateAccident(input) !== null) n++;
    if (this.computeDateConnaissanceEmployeur(input) !== null) n++;
    return n;
  },
} as const;
