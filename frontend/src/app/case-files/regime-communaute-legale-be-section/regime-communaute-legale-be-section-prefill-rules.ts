/**
 * SF-246-28 — Helper partagé pour l'outil "Régime de communauté légale
 * (Belgique)" (`regime-mat-be-communaute-legale`). Module pur — runtime et
 * static appellent les mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * SF-246-28 : levée de `PREFILL_COUNT_ALWAYS_ZERO`. Le pipeline IA extrait
 * désormais 2 champs depuis le sous-objet `famille_be_detection_v2` :
 * `dateMariageBeDetectee` (ISO YYYY-MM-DD) et `contratMariageSigneBeDetecte`
 * (boolean).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface RegimeCommunauteLegaleBePrefillInput {
  aiData?: FamilleExtractedData | null | unknown;
}

/** Date du mariage (ISO YYYY-MM-DD) ou null. */
export function computeDateMariage(
  input: RegimeCommunauteLegaleBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const d = ai.dateMariageBeDetectee;
  if (!d || !ISO_DATE_REGEX.test(d)) return null;
  return d;
}

/** Contrat de mariage signé ou null si absent. */
export function computeContratMariage(
  input: RegimeCommunauteLegaleBePrefillInput,
): boolean | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const v = ai.contratMariageSigneBeDetecte;
  if (v == null) return null;
  return v;
}

/**
 * Compte le nombre de champs pré-remplis par l'IA pour le badge du panneau.
 * SF-246-28 : 2 champs maximum.
 */
export function computePrefillCount(
  input: RegimeCommunauteLegaleBePrefillInput,
): number {
  let count = 0;
  if (computeDateMariage(input) != null) count++;
  if (computeContratMariage(input) != null) count++;
  return count;
}

export const RegimeCommunauteLegaleBeSectionPrefillRules = {
  computePrefillCount,
  computeDateMariage,
  computeContratMariage,
};
