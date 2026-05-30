/**
 * SF-218-12 — Helper partagé pour l'outil "VRP : statut, préavis et indemnité
 * de clientèle" (F-DT-104). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Champs pré-remplissables depuis `TravailExtractedData` (FRANCE uniquement —
 * statut VRP franco-français, art. L.7311-1 et s. CT) :
 *
 *   dateEntree                    ← aiData.dateEntree (existant, ISO)
 *   dateRupture                   ← aiData.dateRuptureContrat ?? dateLicenciement (existant, ISO)
 *   commissionsAnnuellesMoyennes  ← aiData.vrpCommissionsAnnuelles (nouveau, montant)
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface VrpIndemniteClientelePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    'dateEntree' | 'dateRuptureContrat' | 'dateLicenciement' | 'vrpCommissionsAnnuelles'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: VrpIndemniteClientelePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** `dateEntree` — date ISO YYYY-MM-DD. */
export function computeDateEntree(input: VrpIndemniteClientelePrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateEntree;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `dateRupture` — date ISO YYYY-MM-DD (dateRuptureContrat prioritaire, fallback dateLicenciement). */
export function computeDateRupture(input: VrpIndemniteClientelePrefillInput): string | null {
  if (!isFrance(input)) return null;
  const candidates = [input.aiData?.dateRuptureContrat, input.aiData?.dateLicenciement];
  for (const v of candidates) {
    if (typeof v === 'string' && ISO_DATE_REGEX.test(v)) return v;
  }
  return null;
}

/** `commissionsAnnuellesMoyennes` — montant ≥ 0. */
export function computeCommissionsAnnuelles(input: VrpIndemniteClientelePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.vrpCommissionsAnnuelles;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 */
export function computePrefillCount(input: VrpIndemniteClientelePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDateEntree(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  if (computeCommissionsAnnuelles(input) !== null) count++;
  return count;
}

export const VrpIndemniteClienteleSectionPrefillRules = {
  computeDateEntree,
  computeDateRupture,
  computeCommissionsAnnuelles,
  computePrefillCount,
};
