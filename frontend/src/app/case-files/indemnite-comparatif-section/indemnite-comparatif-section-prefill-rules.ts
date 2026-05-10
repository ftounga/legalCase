/**
 * F-236 SF-236-02 — Helper partagé pour le comparateur d'indemnités (F-DT-09).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `IndemniteComparatifSectionComponent.prefillFromAi()` :
 *   `salaireMensuel    ← aiData.salaireBrutMensuel`
 *   `ancienneteAnnees  ← synthesis.compensationEstimate.ancienneteAnnees`
 *   `ancienneteMois    ← synthesis.compensationEstimate.ancienneteMois`
 *   `typeRupture       ← synthesis.compensationEstimate.typeRupture` (si supporté
 *                        par le pays du dossier — FR / BE).
 */

const TYPES_FR_VALUES = new Set<string>(['LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE']);
const TYPES_BE_VALUES = new Set<string>(['LICENCIEMENT_ORDINAIRE']);

export interface IndemniteComparatifPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  synthesis?: {
    compensationEstimate?: {
      ancienneteAnnees?: number | null;
      ancienneteMois?: number | null;
      typeRupture?: string | null;
    } | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuel(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeAncienneteAnnees(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.synthesis?.compensationEstimate?.ancienneteAnnees;
  return typeof v === 'number' ? v : null;
}

export function computeAncienneteMois(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.synthesis?.compensationEstimate?.ancienneteMois;
  return typeof v === 'number' ? v : null;
}

/** Retourne le typeRupture si supporté par le pays, null sinon. */
export function computeTypeRupture(input: IndemniteComparatifPrefillInput): string | null {
  const v = input.synthesis?.compensationEstimate?.typeRupture;
  if (typeof v !== 'string' || v.length === 0) return null;
  const allowed = input.workspaceCountry === 'BELGIQUE' ? TYPES_BE_VALUES : TYPES_FR_VALUES;
  return allowed.has(v) ? v : null;
}

export function computePrefillCount(input: IndemniteComparatifPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuel(input) !== null) count++;
  if (computeAncienneteAnnees(input) !== null) count++;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeTypeRupture(input) !== null) count++;
  return count;
}

export const IndemniteComparatifSectionPrefillRules = {
  computeSalaireMensuel,
  computeAncienneteAnnees,
  computeAncienneteMois,
  computeTypeRupture,
  computePrefillCount,
};
