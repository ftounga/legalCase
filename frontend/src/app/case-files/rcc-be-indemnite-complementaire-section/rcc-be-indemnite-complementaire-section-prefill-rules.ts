import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-07b — Helper partagé pour le pré-remplissage IA de
 * `RccBeIndemniteComplementaireSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 4 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `remunerationNetteReference` ← `aiData.remunerationNetteReferenceRccDetectee`
 *      (number > 0 — > 0 strict, 0 et négatif rejetés car économiquement
 *      incohérent pour une rémunération nette de référence).
 *   2. `allocationOnemMensuelle` ← `aiData.allocationOnemMensuelleEstimee`
 *      (number ≥ 0).
 *   3. `dateNaissanceSalarie` ← `aiData.dateNaissanceSalarie` (déjà extrait
 *      SF-207-06 — ISO YYYY-MM-DD strict).
 *   4. `dateDebutRcc` ← `aiData.dateDebutRccEnvisagee` (ISO YYYY-MM-DD strict).
 *
 * `ageLegalPension` et `planchersSectoriel` ne sont volontairement PAS
 * instrumentés — le défaut backend (66 ans) couvre la majorité des cas
 * 2026, et le plancher sectoriel relève du choix avocat (CCT sectorielle
 * applicable, info non factualisable depuis le dossier salarié).
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

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value <= 0) return null;
  return value;
}

function nonNegativeNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value < 0) return null;
  return value;
}

export const RccBeIndemniteComplementairePrefillRules = {

  /**
   * Rémunération nette de référence : > 0 strict (0 / négatif / non-number → null).
   */
  computeRemunerationNetteReference(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull(
      (ai as { remunerationNetteReferenceRccDetectee?: unknown }).remunerationNetteReferenceRccDetectee,
    );
  },

  /**
   * Allocation ONEM mensuelle : ≥ 0 (0 toléré — allocation absente possible).
   */
  computeAllocationOnemMensuelle(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return nonNegativeNumberOrNull(
      (ai as { allocationOnemMensuelleEstimee?: unknown }).allocationOnemMensuelleEstimee,
    );
  },

  /**
   * Date de naissance : ISO strict YYYY-MM-DD ; toute autre forme → null
   * (parité backend `LocalDate.parse`).
   */
  computeDateNaissanceSalarie(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateNaissanceSalarie?: unknown }).dateNaissanceSalarie);
  },

  /**
   * Date de début RCC : ISO strict YYYY-MM-DD ; toute autre forme → null.
   */
  computeDateDebutRcc(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateDebutRccEnvisagee?: unknown }).dateDebutRccEnvisagee);
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 4.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRemunerationNetteReference(input) !== null) n++;
    if (this.computeAllocationOnemMensuelle(input) !== null) n++;
    if (this.computeDateNaissanceSalarie(input) !== null) n++;
    if (this.computeDateDebutRcc(input) !== null) n++;
    return n;
  },
} as const;
