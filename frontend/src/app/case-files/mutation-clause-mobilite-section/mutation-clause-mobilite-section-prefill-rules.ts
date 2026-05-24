/**
 * SF-212-14 — Helper partagé pour l'outil "Mutation — validité de la clause
 * de mobilité" (F-DT-71). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-13 backend) extrait un sous-objet
 * `mutation_mobilite_detail` (6 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'`
 * — la clause de mobilité est un concept français ; en BE, régime distinct
 * de modification d'un élément essentiel du contrat).
 *
 *   clauseMobilitePresente                  ← aiData.mutationClausePresente
 *   zoneGeographiquePrecise                 ← aiData.mutationZoneGeographiquePrecise
 *   interetLegitimeEmployeur                ← aiData.mutationInteretLegitimeEmployeur
 *   delaiPrevenanceSemaines                 ← aiData.mutationDelaiPrevenanceSemaines
 *   situationFamilialeSalarieContraignante  ← aiData.mutationSituationFamilialeContraingnante
 *   motifMutationProfessionnel              ← aiData.mutationMotifProfessionnel
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface MutationClauseMobilitePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'mutationClausePresente'
    | 'mutationZoneGeographiquePrecise'
    | 'mutationInteretLegitimeEmployeur'
    | 'mutationDelaiPrevenanceSemaines'
    | 'mutationSituationFamilialeContraingnante'
    | 'mutationMotifProfessionnel'
  > | null;
  workspaceCountry?: string;
}

/** Borne supérieure raisonnable du délai de prévenance en semaines. */
const DELAI_PREVENANCE_MAX_SEMAINES = 52;

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: MutationClauseMobilitePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Booléen direct — `clauseMobilitePresente`. */
export function computeClausePresente(
  input: MutationClauseMobilitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationClausePresente;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `zoneGeographiquePrecise`. */
export function computeZonePrecise(
  input: MutationClauseMobilitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationZoneGeographiquePrecise;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `interetLegitimeEmployeur`. */
export function computeInteretLegitime(
  input: MutationClauseMobilitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationInteretLegitimeEmployeur;
  return typeof v === 'boolean' ? v : null;
}

/** Entier — `delaiPrevenanceSemaines`. Borné [0, 52] et integer fini. */
export function computeDelaiPrevenance(
  input: MutationClauseMobilitePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationDelaiPrevenanceSemaines;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > DELAI_PREVENANCE_MAX_SEMAINES) return null;
  return Math.trunc(v);
}

/** Booléen direct — `situationFamilialeSalarieContraignante`. */
export function computeSituationFamiliale(
  input: MutationClauseMobilitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationSituationFamilialeContraingnante;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `motifMutationProfessionnel`. */
export function computeMotifProfessionnel(
  input: MutationClauseMobilitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mutationMotifProfessionnel;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 6 champs IA.
 */
export function computePrefillCount(input: MutationClauseMobilitePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeClausePresente(input) !== null) count++;
  if (computeZonePrecise(input) !== null) count++;
  if (computeInteretLegitime(input) !== null) count++;
  if (computeDelaiPrevenance(input) !== null) count++;
  if (computeSituationFamiliale(input) !== null) count++;
  if (computeMotifProfessionnel(input) !== null) count++;
  return count;
}

export const MutationClauseMobiliteSectionPrefillRules = {
  computeClausePresente,
  computeZonePrecise,
  computeInteretLegitime,
  computeDelaiPrevenance,
  computeSituationFamiliale,
  computeMotifProfessionnel,
  computePrefillCount,
};
