import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { OutplacementBeMotifLicenciement } from '../../core/models/outplacement-be-obligatoire-45.model';

/**
 * SF-207-08b — Helper partagé pour le pré-remplissage IA de
 * `OutplacementBeObligatoire45SectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 5 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `dateLicenciement`        ← `aiData.dateLicenciement`
 *      (ISO YYYY-MM-DD strict, champ déjà extrait FR/BE).
 *   2. `dateNaissanceSalarie`    ← `aiData.dateNaissanceSalarie`
 *      (ISO YYYY-MM-DD strict, livré SF-207-06).
 *   3. `ancienneteAnnees`        ← `aiData.ancienneteSalarie`
 *      (number ≥ 0, livré SF-207-08 backend).
 *   4. `motifLicenciement`       ← `aiData.motifLicenciementDetecte`
 *      (whitelist 4 enum stricte ; toute autre valeur → null).
 *   5. `offreOutplacementRecue`  ← `aiData.offreOutplacementMentionnee`
 *      (boolean — true uniquement pré-remplit ; false / null laissent
 *       la case décochée par défaut).
 *
 * Les 4 champs conditionnels de l'offre (`dateOffreOutplacement`,
 * `offreConformeCCT82`, `salarieAcceptantOffre`) et `contratTempsPlein`
 * ne sont volontairement pas instrumentés : factualisation insuffisante
 * (info absente des documents standards de licenciement BE — décision PO
 * du 2026-05-19 sur la non-pré-remplissabilité, cf.
 * `feedback_decision_tools_all_fields_prefilled.md`).
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

const MOTIF_WHITELIST: ReadonlySet<OutplacementBeMotifLicenciement> = new Set([
  'LICENCIEMENT_ECONOMIQUE',
  'LICENCIEMENT_AUTRE',
  'FAUTE_GRAVE',
  'DEMISSION',
]);

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE.test(trimmed)) return null;
  const parsed = Date.parse(trimmed);
  if (Number.isNaN(parsed)) return null;
  return trimmed;
}

function nonNegativeNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value < 0) return null;
  return value;
}

export const OutplacementBeObligatoire45PrefillRules = {

  computeDateLicenciement(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateLicenciement?: unknown }).dateLicenciement);
  },

  computeDateNaissanceSalarie(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateNaissanceSalarie?: unknown }).dateNaissanceSalarie);
  },

  /**
   * Ancienneté en années (décimales possibles). ≥ 0 ; négatif / non-number
   * → null. Parité backend `BigDecimal` avec `@DecimalMin("0.00")`.
   */
  computeAncienneteAnnees(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return nonNegativeNumberOrNull((ai as { ancienneteSalarie?: unknown }).ancienneteSalarie);
  },

  /**
   * Motif de licenciement : la whitelist stricte est imposée par l'enum
   * backend `OutplacementBeMotifLicenciement`. Toute valeur hors whitelist
   * (vide, casse incorrecte, motif inconnu) → null (l'avocat tranche).
   */
  computeMotifLicenciement(input: PrefillCountInput): OutplacementBeMotifLicenciement | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { motifLicenciementDetecte?: unknown }).motifLicenciementDetecte;
    if (typeof raw !== 'string') return null;
    const candidate = raw.trim() as OutplacementBeMotifLicenciement;
    return MOTIF_WHITELIST.has(candidate) ? candidate : null;
  },

  /**
   * Offre outplacement reçue : true UNIQUEMENT pré-remplit (déclenche
   * l'apparition des 3 champs conditionnels et passe la case à true).
   * `false` / `null` retournent `null` ici → la case reste décochée par
   * défaut côté composant (pas de badge IA — l'absence n'est pas un signal
   * fort).
   */
  computeOffreOutplacementRecue(input: PrefillCountInput): true | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { offreOutplacementMentionnee?: unknown }).offreOutplacementMentionnee;
    return raw === true ? true : null;
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 5.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateLicenciement(input) !== null) n++;
    if (this.computeDateNaissanceSalarie(input) !== null) n++;
    if (this.computeAncienneteAnnees(input) !== null) n++;
    if (this.computeMotifLicenciement(input) !== null) n++;
    if (this.computeOffreOutplacementRecue(input) !== null) n++;
    return n;
  },
} as const;
