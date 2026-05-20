/**
 * SF-246-28 — Helper partagé pour l'outil "Liquidation-partage post-divorce
 * (Belgique)" (`liquidation-partage-be`). Module pur — runtime et static
 * appellent les mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * SF-246-28 : levée de `PREFILL_COUNT_ALWAYS_ZERO`. Le pipeline IA extrait
 * désormais 4 dates ISO depuis le sous-objet `famille_be_detection_v2` :
 * `dateDesignationNotaireBeDetectee`, `dateOuvertureOperationsBeDetectee`,
 * `dateNotificationProjetBeDetectee`, `dateHomologationBeDetectee`.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface LiquidationPartageBePrefillInput {
  aiData?: FamilleExtractedData | null | unknown;
}

/** Date de désignation du notaire (ISO YYYY-MM-DD) ou null. */
export function computeDateDesignationNotaire(
  input: LiquidationPartageBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const d = ai.dateDesignationNotaireBeDetectee;
  if (!d || !ISO_DATE_REGEX.test(d)) return null;
  return d;
}

/** Date d'ouverture des opérations (ISO YYYY-MM-DD) ou null. */
export function computeDateOuvertureOperations(
  input: LiquidationPartageBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const d = ai.dateOuvertureOperationsBeDetectee;
  if (!d || !ISO_DATE_REGEX.test(d)) return null;
  return d;
}

/** Date de notification du projet (ISO YYYY-MM-DD) ou null. */
export function computeDateNotificationProjet(
  input: LiquidationPartageBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const d = ai.dateNotificationProjetBeDetectee;
  if (!d || !ISO_DATE_REGEX.test(d)) return null;
  return d;
}

/** Date d'homologation (ISO YYYY-MM-DD) ou null. */
export function computeDateHomologation(
  input: LiquidationPartageBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const d = ai.dateHomologationBeDetectee;
  if (!d || !ISO_DATE_REGEX.test(d)) return null;
  return d;
}

/**
 * Compte le nombre de dates pré-remplies par l'IA pour le badge du panneau.
 * SF-246-28 : 4 dates maximum.
 */
export function computePrefillCount(
  input: LiquidationPartageBePrefillInput,
): number {
  return [
    computeDateDesignationNotaire(input),
    computeDateOuvertureOperations(input),
    computeDateNotificationProjet(input),
    computeDateHomologation(input),
  ].filter(v => v != null).length;
}

export const LiquidationPartageBeSectionPrefillRules = {
  computePrefillCount,
  computeDateDesignationNotaire,
  computeDateOuvertureOperations,
  computeDateNotificationProjet,
  computeDateHomologation,
};
