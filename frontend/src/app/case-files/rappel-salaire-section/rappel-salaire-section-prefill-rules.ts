/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Rappel de salaire" (FR).
 * SF-246-21 : branchement de 3 champs depuis `paie_detection`
 * (montant versé/mois, période début, période fin).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Note : convention collective non comptée (dépend liste référentielle async).
 */

import { ConventionReferentialService } from '../../core/services/convention-referential.service';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface RappelSalairePrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    conventionCollective?: string | null;
    // SF-246-21 — paie_detection
    rappelSalaireMontantPerverseMensuel?: number | null;
    rappelSalairePeriodeDebut?: string | null;
    rappelSalairePeriodeFin?: string | null;
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

/** SF-246-21 : montant du salaire effectivement versé/mois (€ > 0). Distinct du montant dû. */
export function computeMontantSalairePerverseMensuel(input: RappelSalairePrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.rappelSalaireMontantPerverseMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/** SF-246-21 : date de début de la période de rappel — premier mois impayé (ISO). */
export function computePeriodeDebut(input: RappelSalairePrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.rappelSalairePeriodeDebut;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de fin de la période de rappel — dernier mois impayé (ISO). */
export function computePeriodeFin(input: RappelSalairePrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.rappelSalairePeriodeFin;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

export function computePrefillCount(input: RappelSalairePrefillInput): number {
  let count = 0;
  if (computeMontantSalaireDuMensuel(input) !== null) count++;
  // Convention non comptée (dépend liste référentielle).
  if (computeMontantSalairePerverseMensuel(input) !== null) count++;
  if (computePeriodeDebut(input) !== null) count++;
  if (computePeriodeFin(input) !== null) count++;
  return count;
}

export const RappelSalaireSectionPrefillRules = {
  computeMontantSalaireDuMensuel,
  computeConventionCode,
  computeMontantSalairePerverseMensuel,
  computePeriodeDebut,
  computePeriodeFin,
  computePrefillCount,
};
