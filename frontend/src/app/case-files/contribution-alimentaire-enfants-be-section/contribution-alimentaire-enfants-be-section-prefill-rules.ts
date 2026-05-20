/**
 * SF-246-28 — Helper partagé pour l'outil "Contribution alimentaire des enfants
 * (Belgique)" (`contribution-alimentaire-enfants-be`). Module pur — runtime et
 * static appellent les mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * SF-246-28 : levée de `PREFILL_COUNT_ALWAYS_ZERO`. Le pipeline IA extrait
 * désormais 6 champs depuis le sous-objet `famille_be_detection_v2` :
 * `nombreEnfantsBeDetecte`, `revenuMensuelParent1BeDetecte`,
 * `revenuMensuelParent2BeDetecte`, `allocationsFamilialesMensuellesBeDetectees`,
 * `nuitsHebergementParent1BeDetectees`, `nuitsHebergementParent2BeDetectees`.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface ContributionAlimentaireEnfantsBePrefillInput {
  aiData?: FamilleExtractedData | null | unknown;
}

/** Nombre d'enfants [1, 12] ou null. */
export function computeNombreEnfants(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.nombreEnfantsBeDetecte;
  if (v == null || v < 1 || v > 12) return null;
  return v;
}

/** Revenu mensuel parent 1 (€/mois, ≥ 0) ou null. */
export function computeRevenuParent1(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.revenuMensuelParent1BeDetecte;
  if (v == null || v < 0) return null;
  return v;
}

/** Revenu mensuel parent 2 (€/mois, ≥ 0) ou null. */
export function computeRevenuParent2(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.revenuMensuelParent2BeDetecte;
  if (v == null || v < 0) return null;
  return v;
}

/** Allocations familiales mensuelles (€/mois, ≥ 0) ou null. */
export function computeAllocationsFamiliales(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.allocationsFamilialesMensuellesBeDetectees;
  if (v == null || v < 0) return null;
  return v;
}

/** Nuits d'hébergement parent 1 [0, 30] ou null. */
export function computeNuitsParent1(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.nuitsHebergementParent1BeDetectees;
  if (v == null || v < 0 || v > 30) return null;
  return v;
}

/** Nuits d'hébergement parent 2 [0, 30] ou null. */
export function computeNuitsParent2(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.nuitsHebergementParent2BeDetectees;
  if (v == null || v < 0 || v > 30) return null;
  return v;
}

/**
 * Compte le nombre de champs pré-remplis par l'IA pour le badge du panneau.
 * SF-246-28 : 6 champs maximum.
 */
export function computePrefillCount(
  input: ContributionAlimentaireEnfantsBePrefillInput,
): number {
  return [
    computeNombreEnfants(input),
    computeRevenuParent1(input),
    computeRevenuParent2(input),
    computeAllocationsFamiliales(input),
    computeNuitsParent1(input),
    computeNuitsParent2(input),
  ].filter(v => v != null).length;
}

export const ContributionAlimentaireEnfantsBeSectionPrefillRules = {
  computePrefillCount,
  computeNombreEnfants,
  computeRevenuParent1,
  computeRevenuParent2,
  computeAllocationsFamiliales,
  computeNuitsParent1,
  computeNuitsParent2,
};
