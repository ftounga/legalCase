/**
 * SF-222-01 — Helper partagé pour l'outil "Allocation de soutien familial"
 * (F-FA-ASF-CAF). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-222-01 backend extrait depuis `asf_caf_detection` :
 *  - `parent_isole`              → asfParentIsole
 *  - `pension_fixee`             → asfPensionFixee
 *  - `montant_pension_mensuel`   → asfMontantPension
 *  - `pension_payee`             → asfPensionPayee
 *  - `nb_enfants`                → asfNbEnfants
 *  - `detecte`                   → asfCafDetecte (flag CONTEXTUAL, non remplissable)
 *
 * Pré-fill des 5 champs saisissables (F-246) :
 *   parentIsole               ← aiData.asfParentIsole
 *   pensionFixee              ← aiData.asfPensionFixee
 *   montantPensionMensuel     ← aiData.asfMontantPension
 *   pensionPayee              ← aiData.asfPensionPayee
 *   nbEnfantsACharge          ← aiData.asfNbEnfants
 *
 * FRANCE UNIQUEMENT — outil single-country (art. L. 523-1 CSS).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PensionPayee } from '../../core/models/asf-caf.model';

const PENSION_PAYEE_WHITELIST: ReadonlySet<string> = new Set([
  'NON_PAYEE',
  'PARTIELLE',
  'PAYEE',
]);

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AsfCafPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'asfParentIsole'
    | 'asfPensionFixee'
    | 'asfMontantPension'
    | 'asfPensionPayee'
    | 'asfNbEnfants'
  > | null;
  workspaceCountry?: string;
}

function isFrance(input: AsfCafPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return Math.trunc(v);
}

function boolOrNull(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Parent isolé. */
export function computeParentIsole(input: AsfCafPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.asfParentIsole);
}

/** Pension fixée par titre exécutoire. */
export function computePensionFixee(input: AsfCafPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.asfPensionFixee);
}

/** Montant pension mensuelle (€/mois, ≥ 0). */
export function computeMontantPensionMensuel(input: AsfCafPrefillInput): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.asfMontantPension);
}

/** État de paiement de la pension (whitelist). */
export function computePensionPayee(input: AsfCafPrefillInput): PensionPayee | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.asfPensionPayee;
  if (typeof v !== 'string') return null;
  const up = v.trim().toUpperCase();
  return PENSION_PAYEE_WHITELIST.has(up) ? (up as PensionPayee) : null;
}

/** Nombre d'enfants à charge (≥ 1). */
export function computeNbEnfantsACharge(input: AsfCafPrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = nonNegativeIntOrNull(input.aiData?.asfNbEnfants);
  return v !== null && v >= 1 ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()`. Retourne 0 hors FRANCE.
 * Comptés : parentIsole, pensionFixee, montantPension, pensionPayee, nbEnfants (5).
 */
export function computePrefillCount(input: AsfCafPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeParentIsole(input) !== null) count++;
  if (computePensionFixee(input) !== null) count++;
  if (computeMontantPensionMensuel(input) !== null) count++;
  if (computePensionPayee(input) !== null) count++;
  if (computeNbEnfantsACharge(input) !== null) count++;
  return count;
}

export const AsfCafPrefillRules = {
  computeParentIsole,
  computePensionFixee,
  computeMontantPensionMensuel,
  computePensionPayee,
  computeNbEnfantsACharge,
  computePrefillCount,
};
