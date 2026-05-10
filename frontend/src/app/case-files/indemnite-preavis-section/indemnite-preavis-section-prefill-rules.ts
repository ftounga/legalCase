/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Indemnité de préavis" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Note : la sélection de convention collective dépend d'une liste référentielle
 * chargée par le service au runtime ; côté static on ne peut donc pas vérifier
 * que la valeur IA matche cette liste. La convention n'est pas comptée par
 * `computePrefillCount` (graceful — sous-estimation possible de 1).
 */

import { ConventionReferentialService } from '../../core/services/convention-referential.service';

export interface IndemnitePreavisPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    dateLicenciement?: string | null;
    conventionCollective?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: IndemnitePreavisPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateRupture(input: IndemnitePreavisPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/**
 * Code de convention collective normalisé via le service — runtime confirmera
 * l'existence dans la liste référentielle (non comptée dans `computePrefillCount`).
 */
export function computeConventionCode(input: IndemnitePreavisPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.conventionCollective;
  if (typeof v !== 'string' || v.length === 0) return null;
  return ConventionReferentialService.normalizeCode(v);
}

export function computePrefillCount(input: IndemnitePreavisPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  // Convention non comptée (dépend de la liste référentielle async runtime).
  return count;
}

export const IndemnitePreavisSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDateRupture,
  computeConventionCode,
  computePrefillCount,
};
