import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RappelSalaireBeTypeArriere } from '../../core/models/rappel-salaire-be.model';

/**
 * SF-213-02b — Helper partagé pour le pré-remplissage IA de
 * `RappelSalaireBeSectionComponent` (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 4 champs pré-remplissables depuis `TravailExtractedData` branche BE :
 *   1. `montantBrut`        ← `aiData.montantArrieresSalaireBrut` (number > 0).
 *   2. `dateDebutPeriode`   ← `aiData.dateDebutArrieresSalaire`   (ISO date).
 *   3. `dateFinPeriode`     ← `aiData.dateFinArrieresSalaire`     (ISO date).
 *   4. `dateRupture`        ← `aiData.dateRuptureContrat`         (ISO date).
 *
 * `typeArriereEnum` est dérivé du `dateRupture` (PENDANT_CONTRAT ou
 * POST_RUPTURE) — il n'est PAS compté séparément dans `computePrefillCount`
 * (cf. mini-spec §"Pré-fill rules" — max théorique = 4).
 *
 * Gating : `workspaceCountry !== 'BELGIQUE'` → null (outil mono-pays BE,
 * Loi 12/04/1965 + Loi 03/07/1978 art. 15 — pas d'équivalent FR direct).
 */

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value <= 0) return null;
  return value;
}

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE_REGEX.test(trimmed)) return null;
  // Validation supplémentaire : la date doit être parseable (ex. 2025-02-31 rejeté).
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) return null;
  // Vérifie que la string ISO produite repart sur les mêmes Y-M-D
  // (évite 2025-02-31 qui Date.parse interpréterait en 2025-03-03).
  if (parsed.toISOString().slice(0, 10) !== trimmed) return null;
  return trimmed;
}

export const RappelSalaireBeSectionPrefillRules = {

  /** Montant brut réclamé : > 0 strict. Parité backend `@DecimalMin > 0`. */
  computeMontantBrut(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull((ai as { montantArrieresSalaireBrut?: unknown }).montantArrieresSalaireBrut);
  },

  /** Date début période d'arriéré : ISO YYYY-MM-DD strict. */
  computeDateDebutPeriode(input: PrefillCountInput): string | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateDebutArrieresSalaire?: unknown }).dateDebutArrieresSalaire);
  },

  /** Date fin période d'arriéré : ISO YYYY-MM-DD strict. */
  computeDateFinPeriode(input: PrefillCountInput): string | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateFinArrieresSalaire?: unknown }).dateFinArrieresSalaire);
  },

  /** Date de rupture du contrat (réutilise SF-207-01 `dateRuptureContrat`). */
  computeDateRupture(input: PrefillCountInput): string | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateRuptureContrat?: unknown }).dateRuptureContrat);
  },

  /**
   * Dérive le `typeArriereEnum` à partir de la présence de la `dateRupture` IA.
   * Si `dateRupture` est extraite avec une valeur ISO valide → `POST_RUPTURE`,
   * sinon `PENDANT_CONTRAT` (régime majoritaire). Le mode `MIXTE` reste à la
   * main de l'avocat (jugement métier — le contrat n'est jamais MIXTE par défaut).
   *
   * Retourne `null` si gate FRANCE ou aiData absent. Cette dérivation n'est PAS
   * comptée dans `computePrefillCount` (cf. mini-spec §"Pré-fill rules").
   */
  computeTypeArriereEnum(input: PrefillCountInput): RappelSalaireBeTypeArriere | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateRupture = isoDateOrNull((ai as { dateRuptureContrat?: unknown }).dateRuptureContrat);
    return dateRupture !== null ? 'POST_RUPTURE' : 'PENDANT_CONTRAT';
  },

  /**
   * Maître : compte les 4 champs saisis non-null pré-remplissables depuis l'IA.
   * `typeArriereEnum` est dérivé (pas un champ IA direct) → non compté.
   * Max théorique = 4.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeMontantBrut(input) !== null) n++;
    if (this.computeDateDebutPeriode(input) !== null) n++;
    if (this.computeDateFinPeriode(input) !== null) n++;
    if (this.computeDateRupture(input) !== null) n++;
    return n;
  },
} as const;
