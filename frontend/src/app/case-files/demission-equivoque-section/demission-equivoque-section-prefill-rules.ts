/**
 * SF-212-22 / F-256 — Helper partagé pour l'outil « Démission — validité et
 * caractère équivoque » (F-DT-41). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237 / F-246).
 *
 * Pré-fill IA réactivé par F-256 : le sous-objet `demission_equivoque_detail`
 * est désormais projeté sur `TravailExtractedData` côté backend.
 *
 *   modeExpressionDemission       ← aiData.demissionEquivoqueDetail.demissionModeExpression
 *   contexteAltercation           ← aiData.demissionEquivoqueDetail.demissionContexteAltercation
 *   pression                      ← aiData.demissionEquivoqueDetail.demissionPression
 *   retractation                  ← aiData.demissionEquivoqueDetail.demissionRetractation
 *   manquementsEmployeur          ← aiData.demissionEquivoqueDetail.demissionManquementsEmployeur
 *
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — exigence prétorienne
 * Cass. soc. 09/05/2007 propre au droit français).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface DemissionEquivoquePrefillInput {
  aiData?: Pick<TravailExtractedData, 'demissionEquivoqueDetail'> | null;
  workspaceCountry?: string;
}

function isFrance(input: DemissionEquivoquePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Texte libre — mode d'expression de la démission. */
export function computeModeExpression(input: DemissionEquivoquePrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.demissionEquivoqueDetail?.demissionModeExpression;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/** Booléen — contexte d'altercation. */
export function computeContexteAltercation(input: DemissionEquivoquePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.demissionEquivoqueDetail?.demissionContexteAltercation;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen — pression employeur. */
export function computePression(input: DemissionEquivoquePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.demissionEquivoqueDetail?.demissionPression;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen — rétractation rapide produite par le salarié. */
export function computeRetractation(input: DemissionEquivoquePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.demissionEquivoqueDetail?.demissionRetractation;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen — manquements employeur contemporains. */
export function computeManquementsEmployeur(input: DemissionEquivoquePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.demissionEquivoqueDetail?.demissionManquementsEmployeur;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si non-FRANCE.
 * Couvre les 5 champs IA du sous-objet `demissionEquivoqueDetail`.
 */
export function computePrefillCount(input: DemissionEquivoquePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeModeExpression(input) !== null) count++;
  if (computeContexteAltercation(input) !== null) count++;
  if (computePression(input) !== null) count++;
  if (computeRetractation(input) !== null) count++;
  if (computeManquementsEmployeur(input) !== null) count++;
  return count;
}

export const DemissionEquivoqueSectionPrefillRules = {
  computeModeExpression,
  computeContexteAltercation,
  computePression,
  computeRetractation,
  computeManquementsEmployeur,
  computePrefillCount,
};
