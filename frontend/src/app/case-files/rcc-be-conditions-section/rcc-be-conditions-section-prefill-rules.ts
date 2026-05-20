import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-06b — Helper partagé pour le pré-remplissage IA de
 * `RccBeConditionsSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 4 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `dateNaissance` ← `aiData.dateNaissanceSalarie` (ISO YYYY-MM-DD strict).
 *   2. `anneesCarriereProfessionnelle` ← `aiData.anneesCarriereSalarie`
 *      (Integer, borné [0, 60]).
 *   3. `metierLourd` ← `aiData.metierLourdDetecte` (true seulement — `false`
 *      ne déclenche pas de pré-fill).
 *   4. `entrepriseEnDifficulte` ← `aiData.entrepriseEnDifficulteDetectee`
 *      (true seulement — `false` ne déclenche pas de pré-fill).
 *
 * `longueCarriere` n'est volontairement pas instrumenté — c'est un jugement
 * de l'avocat sur la prise en compte effective de la carrière complète
 * (régularisations, périodes assimilées) que l'IA ne peut pas trancher.
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

function clampedIntegerOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (!Number.isInteger(value)) return null;
  if (value < 0 || value > 60) return null;
  return value;
}

export const RccBeConditionsPrefillRules = {

  /**
   * Date de naissance : ISO strict YYYY-MM-DD ; toute autre forme → null
   * (parité backend `LocalDate.parse`).
   */
  computeDateNaissance(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateNaissanceSalarie?: unknown }).dateNaissanceSalarie);
  },

  /**
   * Années de carrière : entier dans [0, 60]. Toute valeur hors borne ou
   * non-entière → null.
   */
  computeAnneesCarriere(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return clampedIntegerOrNull((ai as { anneesCarriereSalarie?: unknown }).anneesCarriereSalarie);
  },

  /**
   * Métier lourd : pré-fill UNIQUEMENT si l'IA a positivement détecté un métier
   * lourd (true). `false` / `null` / `undefined` / autre → null.
   */
  computeMetierLourd(input: PrefillCountInput): true | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { metierLourdDetecte?: unknown }).metierLourdDetecte;
    return raw === true ? true : null;
  },

  /**
   * Entreprise en difficulté : pré-fill UNIQUEMENT si l'IA a positivement
   * détecté une reconnaissance (true). `false` / `null` / `undefined` / autre → null.
   */
  computeEntrepriseEnDifficulte(input: PrefillCountInput): true | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { entrepriseEnDifficulteDetectee?: unknown }).entrepriseEnDifficulteDetectee;
    return raw === true ? true : null;
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 4.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateNaissance(input) !== null) n++;
    if (this.computeAnneesCarriere(input) !== null) n++;
    if (this.computeMetierLourd(input) !== null) n++;
    if (this.computeEntrepriseEnDifficulte(input) !== null) n++;
    return n;
  },
} as const;
