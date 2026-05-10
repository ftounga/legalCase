/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Indemnité rupture conventionnelle".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `salaireMensuel    ← aiData.salaireBrutMensuel > 0`
 *                        fallback `synthesis.compensationEstimate.salaireReference`
 *   `ancienneteAnnees  ← synthesis.compensationEstimate.ancienneteAnnees`
 */

export interface RuptureConvIndemnitePrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  synthesis?: {
    compensationEstimate?: {
      salaireReference?: number | null;
      ancienneteAnnees?: number | null;
    } | null;
  } | null;
}

export function computeSalaireMensuel(input: RuptureConvIndemnitePrefillInput): number | null {
  const ia = input.aiData?.salaireBrutMensuel;
  const estim = input.synthesis?.compensationEstimate?.salaireReference;
  const iaSalaire = typeof ia === 'number' && ia > 0 ? ia : null;
  const estimSalaire = typeof estim === 'number' && estim > 0 ? estim : null;
  return iaSalaire ?? estimSalaire;
}

export function computeAncienneteAnnees(input: RuptureConvIndemnitePrefillInput): number | null {
  const v = input.synthesis?.compensationEstimate?.ancienneteAnnees;
  return typeof v === 'number' ? v : null;
}

export function computePrefillCount(input: RuptureConvIndemnitePrefillInput): number {
  let count = 0;
  if (computeSalaireMensuel(input) !== null) count++;
  if (computeAncienneteAnnees(input) !== null) count++;
  return count;
}

export const RuptureConvIndemniteSectionPrefillRules = {
  computeSalaireMensuel,
  computeAncienneteAnnees,
  computePrefillCount,
};
