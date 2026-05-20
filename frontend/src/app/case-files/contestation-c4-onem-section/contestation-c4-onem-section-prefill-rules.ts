import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-03b — Helper partagé pour le pré-remplissage IA de
 * `ContestationC4OnemSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 3 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `dateNotificationDecisionOnem` ← `aiData.dateNotificationDecisionOnem`
 *   2. `dateDecisionDirecteur` ← `aiData.dateDecisionDirecteur`
 *   3. `recoursAdminDejaForme` ← `aiData.recoursAdminDejaForme` (Boolean
 *      nullable côté backend) — pré-fill SEULEMENT si non-null (true OU false
 *      provenant de l'IA = signal exploitable).
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

function booleanOrNull(value: unknown): boolean | null {
  if (value === true) return true;
  if (value === false) return false;
  return null;
}

export const ContestationC4OnemPrefillRules = {

  computeDateNotificationDecisionOnem(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateNotificationDecisionOnem);
  },

  computeDateDecisionDirecteur(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateDecisionDirecteur);
  },

  /**
   * Pré-fill booléen — accepte true OU false. Null si non exposé par l'IA
   * (cas le plus fréquent en V1 : l'IA n'est pas obligée d'expliciter).
   */
  computeRecoursAdminDejaForme(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    return booleanOrNull(ai.recoursAdminDejaForme);
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec). Max = 3.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateNotificationDecisionOnem(input) !== null) n++;
    if (this.computeDateDecisionDirecteur(input) !== null) n++;
    if (this.computeRecoursAdminDejaForme(input) !== null) n++;
    return n;
  },
} as const;
