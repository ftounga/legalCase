/**
 * SF-216-02 — Helper partagé pour l'outil "Prestation compensatoire" (F-FA-01).
 * Module pur — runtime (`prefillFromAi()`) et static (`getPrefillCount()`)
 * appellent les MÊMES fonctions (contrat F-236 / F-237 / F-246).
 *
 * Le pipeline IA SF-216-01 backend extrait :
 *  - `vie_commune_detection.duree_mariage_annees_fr` (déjà branché F-246)
 *  - `vie_commune_detection.revenus_annuels_epoux1_eur` (déjà branché F-246)
 *  - `vie_commune_detection.revenus_annuels_epoux2_eur` (déjà branché F-246)
 *  - `vie_commune_detection.age_epoux1_annees`        (NOUVEAU SF-216-01)
 *  - `vie_commune_detection.age_epoux2_annees`        (NOUVEAU SF-216-01)
 *  - `communaute_partage_protection_detection_v2.clause_attribution_integrale_detected`
 *    → flag `avantageMatrimonialDetecte` (déjà branché F-246).
 *
 * Le record Java `FamilleExtractedData` projette ces 5 champs à plat dans le
 * `familleExtractedData` du synthesis frontend. Noms Jackson :
 *   `dureeMariageAnnees`, `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`,
 *   `ageEpoux1Annees`, `ageEpoux2Annees`, `clauseAttributionIntegraleDetected`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 270 Cciv).
 *
 *   dureeMariageAnnees           ← aiData.dureeMariageAnnees
 *   revenusAnnuelsEpoux1Eur      ← aiData.revenusAnnuelsEpoux1
 *   revenusAnnuelsEpoux2Eur      ← aiData.revenusAnnuelsEpoux2
 *   ageEpoux1Annees              ← aiData.ageEpoux1Annees
 *   ageEpoux2Annees              ← aiData.ageEpoux2Annees
 *   avantageMatrimonialDetecte   ← aiData.clauseAttributionIntegraleDetected
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface PrestationCompensatoirePrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'dureeMariageAnnees'
    | 'revenusAnnuelsEpoux1'
    | 'revenusAnnuelsEpoux2'
    | 'ageEpoux1Annees'
    | 'ageEpoux2Annees'
    | 'clauseAttributionIntegraleDetected'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: PrestationCompensatoirePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return v;
}

function boolOrNull(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Durée du mariage en années entières [0, 80]. */
export function computeDureeMariageAnnees(
  input: PrestationCompensatoirePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.dureeMariageAnnees);
}

/** Revenus annuels époux 1 (€/an) — backend Jackson : `revenusAnnuelsEpoux1`. */
export function computeRevenusEpoux1(
  input: PrestationCompensatoirePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.revenusAnnuelsEpoux1);
}

/** Revenus annuels époux 2 (€/an). */
export function computeRevenusEpoux2(
  input: PrestationCompensatoirePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.revenusAnnuelsEpoux2);
}

/** Âge époux 1 en années entières [0, 120]. */
export function computeAgeEpoux1(
  input: PrestationCompensatoirePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.ageEpoux1Annees);
}

/** Âge époux 2 en années entières [0, 120]. */
export function computeAgeEpoux2(
  input: PrestationCompensatoirePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.ageEpoux2Annees);
}

/**
 * Avantage matrimonial détecté — la clause d'attribution intégrale de la
 * communauté universelle (art. 1527 al. 1 Cciv) est un cas typique
 * d'avantage matrimonial pris en compte pour moduler la prestation.
 */
export function computeAvantageMatrimonial(
  input: PrestationCompensatoirePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.clauseAttributionIntegraleDetected);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 6 champs IA (5 nouveaux SF-216-01 + 1 hérité F-246).
 */
export function computePrefillCount(
  input: PrestationCompensatoirePrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDureeMariageAnnees(input) !== null) count++;
  if (computeRevenusEpoux1(input) !== null) count++;
  if (computeRevenusEpoux2(input) !== null) count++;
  if (computeAgeEpoux1(input) !== null) count++;
  if (computeAgeEpoux2(input) !== null) count++;
  if (computeAvantageMatrimonial(input) !== null) count++;
  return count;
}

export const PrestationCompensatoireSectionPrefillRules = {
  computeDureeMariageAnnees,
  computeRevenusEpoux1,
  computeRevenusEpoux2,
  computeAgeEpoux1,
  computeAgeEpoux2,
  computeAvantageMatrimonial,
  computePrefillCount,
};
