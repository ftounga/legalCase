/**
 * SF-216-10 — Helper partagé pour l'outil "Délégation d'autorité parentale"
 * (F-FA-XX-delegation-ap). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-216-09 backend extrait :
 *  - `delegation_ap_detection.envisagee`           (flag CONTEXTUAL, non remplissable)
 *  - `delegation_ap_detection.tiers_lien_familial` (NOUVEAU SF-216-09)
 *  - `delegation_ap_detection.accord_parents`      (NOUVEAU SF-216-09)
 *  - `filiation_detection_v2.agesEnfantsDetectes`  (déjà branché F-246)
 *
 * Le record Java `FamilleExtractedData` projette ces champs à plat dans le
 * `familleExtractedData` du synthesis frontend. Noms Jackson :
 *   `delegationApEnvisagee`, `tiersLienFamilialDetecte`,
 *   `accordParentsDetecte`, `agesEnfantsDetectes`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 376-1 Cciv).
 *
 *   ageEnfant           ← aiData.agesEnfantsDetectes[0]
 *   tiersLienFamilial   ← aiData.tiersLienFamilialDetecte
 *   accordParents       ← aiData.accordParentsDetecte
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { TiersLienFamilial } from '../../core/models/delegation-ap-fr.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface DelegationApFrPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'agesEnfantsDetectes'
    | 'tiersLienFamilialDetecte'
    | 'accordParentsDetecte'
  > | null;
  workspaceCountry?: string;
}

const TIERS_LIEN_WHITELIST: ReadonlySet<TiersLienFamilial> = new Set<TiersLienFamilial>([
  'GRANDS_PARENTS',
  'ONCLE_TANTE',
  'FAMILLE_ELARGIE',
  'ASSOCIATION_HABILITEE',
  'AUTRE',
]);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: DelegationApFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  const i = Math.trunc(v);
  return i > 18 ? null : i;
}

/**
 * Âge du premier enfant détecté dans `filiation_detection_v2.agesEnfantsDetectes`.
 * Borné [0, 18] (mineurs uniquement — art. 371-1 Cciv).
 */
export function computeAgeEnfant(
  input: DelegationApFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages) || ages.length === 0) return null;
  return nonNegativeIntOrNull(ages[0]);
}

/** Lien familial du tiers détecté dans le dossier (whitelist 5 valeurs). */
export function computeTiersLienFamilial(
  input: DelegationApFrPrefillInput,
): TiersLienFamilial | null {
  if (!isFrance(input)) return null;
  const raw = input.aiData?.tiersLienFamilialDetecte;
  if (typeof raw !== 'string') return null;
  const upper = raw.toUpperCase();
  return TIERS_LIEN_WHITELIST.has(upper as TiersLienFamilial)
    ? (upper as TiersLienFamilial)
    : null;
}

/** Accord des deux parents détecté dans le dossier (booléen strict). */
export function computeAccordParents(
  input: DelegationApFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.accordParentsDetecte;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 *
 * Comptés ici : ageEnfant, tiersLienFamilial, accordParents (3 champs).
 */
export function computePrefillCount(
  input: DelegationApFrPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAgeEnfant(input) !== null) count++;
  if (computeTiersLienFamilial(input) !== null) count++;
  if (computeAccordParents(input) !== null) count++;
  return count;
}

export const DelegationApFrPrefillRules = {
  computeAgeEnfant,
  computeTiersLienFamilial,
  computeAccordParents,
  computePrefillCount,
};
