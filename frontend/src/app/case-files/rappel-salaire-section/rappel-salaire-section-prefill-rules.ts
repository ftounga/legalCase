/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Rappel de salaire" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Note : convention collective non comptée (dépend liste référentielle async).
 */

import { ConventionReferentialService } from '../../core/services/convention-referential.service';

export interface RappelSalairePrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    conventionCollective?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeMontantSalaireDuMensuel(input: RappelSalairePrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeConventionCode(input: RappelSalairePrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.conventionCollective;
  if (typeof v !== 'string' || v.length === 0) return null;
  return ConventionReferentialService.normalizeCode(v);
}

export function computePrefillCount(input: RappelSalairePrefillInput): number {
  let count = 0;
  if (computeMontantSalaireDuMensuel(input) !== null) count++;
  // Convention non comptée (dépend liste référentielle).
  return count;
}

export const RappelSalaireSectionPrefillRules = {
  computeMontantSalaireDuMensuel,
  computeConventionCode,
  computePrefillCount,
};
