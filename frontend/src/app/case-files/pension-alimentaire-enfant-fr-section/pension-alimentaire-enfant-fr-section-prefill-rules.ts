/**
 * SF-216-04 — Helper partagé pour l'outil "Pension alimentaire enfant FR" (F-FA-02).
 * Module pur — runtime (`prefillFromAi()`) et static (`getPrefillCount()`)
 * appellent les MÊMES fonctions (contrat F-236 / F-237 / F-246).
 *
 * Le pipeline IA SF-216-03 backend extrait :
 *  - `vie_commune_detection.revenus_annuels_epoux1_eur` (F-246, projeté `revenusAnnuelsEpoux1` /year)
 *  - `vie_commune_detection.revenus_annuels_epoux2_eur` (F-246, projeté `revenusAnnuelsEpoux2` /year)
 *  - `filiation_detection_v2.nombre_enfants_detecte`    (F-246, projeté `nombreEnfantsDetecte`)
 *  - `filiation_detection_v2.ages_enfants_detectes`     (F-246, projeté `agesEnfantsDetectes`)
 *  - `pension_alimentaire_detection.envisagee`          (NOUVEAU SF-216-03, flag CONTEXTUAL)
 *  - `pension_alimentaire_detection.mode_residence`     (NOUVEAU SF-216-03, enum)
 *
 * Le record Java `FamilleExtractedData` projette ces champs à plat. Noms Jackson :
 *   `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`, `nombreEnfantsDetecte`,
 *   `agesEnfantsDetectes`, `pensionAlimentaireEnvisagee`, `modeResidenceEnfantsDetecte`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 371-2 Cciv).
 *
 * Conversion annuel → mensuel : revenus annuels / 12 (arrondi).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ModeResidenceEnfant } from '../../core/models/pension-alimentaire-enfant-fr.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface PensionAlimentaireEnfantFrPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'revenusAnnuelsEpoux1'
    | 'revenusAnnuelsEpoux2'
    | 'nbEnfantsACharge'
    | 'agesEnfantsDetectes'
    | 'modeResidenceEnfantsDetecte'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: PensionAlimentaireEnfantFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return Math.trunc(v);
}

/**
 * Revenus nets mensuels parent 1 (€, ≥ 0) — conversion annuel → mensuel.
 * Lecture de `revenusAnnuelsEpoux1` (€/an) divisé par 12.
 */
export function computeRevenusNetsParent1(
  input: PensionAlimentaireEnfantFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const annuel = input.aiData?.revenusAnnuelsEpoux1;
  if (typeof annuel !== 'number' || !Number.isFinite(annuel) || annuel < 0) return null;
  return Math.round(annuel / 12);
}

/** Revenus nets mensuels parent 2 (€, ≥ 0) — idem parent 1. */
export function computeRevenusNetsParent2(
  input: PensionAlimentaireEnfantFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const annuel = input.aiData?.revenusAnnuelsEpoux2;
  if (typeof annuel !== 'number' || !Number.isFinite(annuel) || annuel < 0) return null;
  return Math.round(annuel / 12);
}

/**
 * Nombre d'enfants détecté (entier ≥ 1). Lecture du champ canonique
 * `vie_commune_detection.nb_enfants_a_charge` (projeté `nbEnfantsACharge`).
 */
export function computeNombreEnfants(
  input: PensionAlimentaireEnfantFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const n = input.aiData?.nbEnfantsACharge;
  if (typeof n !== 'number' || !Number.isFinite(n) || n < 1) return null;
  return Math.trunc(n);
}

/** Liste des âges des enfants détectés (entiers ≥ 0, doit aligner sur `nombreEnfants`). */
export function computeAgesEnfants(
  input: PensionAlimentaireEnfantFrPrefillInput,
): number[] | null {
  if (!isFrance(input)) return null;
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages) || ages.length === 0) return null;
  const cleaned = ages
    .map((a) => nonNegativeIntOrNull(typeof a === 'number' ? a : null))
    .filter((a): a is number => a !== null);
  return cleaned.length > 0 ? cleaned : null;
}

const MODE_WHITELIST: ReadonlySet<string> = new Set([
  'ALTERNEE', 'PRINCIPALE_PARENT1', 'PRINCIPALE_PARENT2',
]);

/** Mode de résidence des enfants détecté (whitelisté). */
export function computeModeResidence(
  input: PensionAlimentaireEnfantFrPrefillInput,
): ModeResidenceEnfant | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.modeResidenceEnfantsDetecte;
  if (typeof v !== 'string' || v.length === 0) return null;
  return MODE_WHITELIST.has(v) ? (v as ModeResidenceEnfant) : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 *
 * Comptés (5 champs) : revenus parent 1, revenus parent 2, nombre enfants,
 * âges enfants (compté pour 1, indépendamment de la taille), mode résidence.
 */
export function computePrefillCount(
  input: PensionAlimentaireEnfantFrPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeRevenusNetsParent1(input) !== null) count++;
  if (computeRevenusNetsParent2(input) !== null) count++;
  if (computeNombreEnfants(input) !== null) count++;
  if (computeAgesEnfants(input) !== null) count++;
  if (computeModeResidence(input) !== null) count++;
  return count;
}

export const PensionAlimentaireEnfantFrPrefillRules = {
  computeRevenusNetsParent1,
  computeRevenusNetsParent2,
  computeNombreEnfants,
  computeAgesEnfants,
  computeModeResidence,
  computePrefillCount,
};
