/**
 * SF-212-18 / F-256 — Helper partagé pour l'outil « Rupture anticipée du CDD »
 * (F-DT-43). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Pré-fill IA réactivé par F-256 : le sous-objet `rupture_anticipee_cdd_detail`
 * est désormais projeté à plat sur `TravailExtractedData` côté backend (slot
 * libéré par le refactor TravailExtractedData en sous-records).
 *
 *   auteurRupture   ← aiData.ruptureAnticipeeCddDetail.ruptureAnticipeeCddAuteur
 *   motifRupture    ← aiData.ruptureAnticipeeCddDetail.ruptureAnticipeeCddMotif
 *   dateTermeCdd    ← aiData.ruptureAnticipeeCddDetail.ruptureAnticipeeCddDateTerme
 *
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'`).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Auteur de la rupture anticipée (enum F-DT-43). */
export type RacAuteurRuptureValue = 'EMPLOYEUR' | 'SALARIE';

/** Motif invoqué (enum F-DT-43). */
export type RacMotifRuptureValue =
  | 'ACCORD_PARTIES'
  | 'FAUTE_GRAVE'
  | 'FORCE_MAJEURE'
  | 'INAPTITUDE'
  | 'CDI_EMBAUCHE'
  | 'AUTRE';

const AUTEUR_VALEURS: ReadonlySet<RacAuteurRuptureValue> = new Set(['EMPLOYEUR', 'SALARIE']);
const MOTIF_VALEURS: ReadonlySet<RacMotifRuptureValue> = new Set([
  'ACCORD_PARTIES',
  'FAUTE_GRAVE',
  'FORCE_MAJEURE',
  'INAPTITUDE',
  'CDI_EMBAUCHE',
  'AUTRE',
]);

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface RuptureAnticipeeCddPrefillInput {
  aiData?: Pick<TravailExtractedData, 'ruptureAnticipeeCddDetail'> | null;
  workspaceCountry?: string;
}

function isFrance(input: RuptureAnticipeeCddPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum — `auteurRupture`. */
export function computeAuteur(input: RuptureAnticipeeCddPrefillInput): RacAuteurRuptureValue | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.ruptureAnticipeeCddDetail?.ruptureAnticipeeCddAuteur;
  if (typeof v !== 'string') return null;
  return AUTEUR_VALEURS.has(v as RacAuteurRuptureValue) ? (v as RacAuteurRuptureValue) : null;
}

/** Enum — `motifRupture`. */
export function computeMotif(input: RuptureAnticipeeCddPrefillInput): RacMotifRuptureValue | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.ruptureAnticipeeCddDetail?.ruptureAnticipeeCddMotif;
  if (typeof v !== 'string') return null;
  return MOTIF_VALEURS.has(v as RacMotifRuptureValue) ? (v as RacMotifRuptureValue) : null;
}

/** Date ISO YYYY-MM-DD — `dateTermeCdd`. */
export function computeDateTerme(input: RuptureAnticipeeCddPrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.ruptureAnticipeeCddDetail?.ruptureAnticipeeCddDateTerme;
  if (typeof v !== 'string') return null;
  // Format ISO simple YYYY-MM-DD
  return /^\d{4}-\d{2}-\d{2}$/.test(v) ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si non-FRANCE.
 */
export function computePrefillCount(input: RuptureAnticipeeCddPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAuteur(input) !== null) count++;
  if (computeMotif(input) !== null) count++;
  if (computeDateTerme(input) !== null) count++;
  return count;
}

export const RuptureAnticipeeCddSectionPrefillRules = {
  computeAuteur,
  computeMotif,
  computeDateTerme,
  computePrefillCount,
};
