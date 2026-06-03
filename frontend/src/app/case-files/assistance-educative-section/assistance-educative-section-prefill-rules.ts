/**
 * SF-222-04 — Helper partagé pour l'outil "Assistance éducative" (mineur en
 * danger — F-FA-ASSISTANCE-EDUCATIVE). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * Le pipeline IA SF-222-04 backend extrait depuis `assistance_educative_detection` :
 *  - `danger_caracterise`                 → aeDangerCaracterise
 *  - `urgence`                            → aeUrgence
 *  - `adhesion_famille`                   → aeAdhesionFamille
 *  - `maintien_milieu_familial_possible`  → aeMaintienMilieu
 *  - `mesure_amiable_ase_envisageable`    → aeMesureAmiable
 *  - `detecte`                            → assistanceEducativeDetectee (flag CONTEXTUAL, non remplissable)
 *
 * Pré-fill des 5 critères saisissables (F-246) :
 *   dangerCaracterise               ← aiData.aeDangerCaracterise
 *   urgence                         ← aiData.aeUrgence
 *   adhesionFamille                 ← aiData.aeAdhesionFamille
 *   maintienMilieuFamilialPossible  ← aiData.aeMaintienMilieu
 *   mesureAmiableASEEnvisageable    ← aiData.aeMesureAmiable
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 375 et s. Cciv).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AssistanceEducativePrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'aeDangerCaracterise'
    | 'aeUrgence'
    | 'aeAdhesionFamille'
    | 'aeMaintienMilieu'
    | 'aeMesureAmiable'
  > | null;
  workspaceCountry?: string;
}

function isFrance(input: AssistanceEducativePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function boolOrNull(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Danger caractérisé (art. 375 Cciv). */
export function computeDanger(input: AssistanceEducativePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.aeDangerCaracterise);
}

/** Urgence — danger immédiat (art. 375-5 Cciv). */
export function computeUrgence(input: AssistanceEducativePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.aeUrgence);
}

/** Adhésion des titulaires de l'autorité parentale. */
export function computeAdhesion(input: AssistanceEducativePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.aeAdhesionFamille);
}

/** Maintien dans le milieu familial possible (art. 375-2 Cciv). */
export function computeMaintien(input: AssistanceEducativePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.aeMaintienMilieu);
}

/** Mesure amiable ASE (AED) envisageable (art. L. 222-3 CASF). */
export function computeMesureAmiable(input: AssistanceEducativePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.aeMesureAmiable);
}

/**
 * Nombre exact de critères pré-remplissables — parité stricte avec
 * `prefillFromAi()`. Retourne 0 hors FRANCE.
 * Comptés : danger, urgence, adhesion, maintien, mesureAmiable (5).
 */
export function computePrefillCount(input: AssistanceEducativePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDanger(input) !== null) count++;
  if (computeUrgence(input) !== null) count++;
  if (computeAdhesion(input) !== null) count++;
  if (computeMaintien(input) !== null) count++;
  if (computeMesureAmiable(input) !== null) count++;
  return count;
}

export const AssistanceEducativePrefillRules = {
  computeDanger,
  computeUrgence,
  computeAdhesion,
  computeMaintien,
  computeMesureAmiable,
  computePrefillCount,
};
