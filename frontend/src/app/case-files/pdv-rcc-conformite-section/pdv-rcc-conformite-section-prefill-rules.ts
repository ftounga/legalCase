/**
 * SF-212-36 — Helper partagé pour l'outil "PDV / RCC — conformité" (F-DT-46).
 * Module pur — runtime (`prefillFromAi()`) et static (`getPrefillCount()`)
 * appellent les MÊMES fonctions (contrat F-236 / F-237 / F-246).
 *
 * F-256 : le sous-objet IA `pdvRccDetail` est désormais projeté côté backend
 * (refactor TravailExtractedData en sous-records — slot libéré). Les 4
 * champs sont pré-remplis depuis le pipeline IA si documentés dans les pièces.
 *
 *   typeDispositif                  ← aiData.pdvRccDetail.pdvRccTypeDispositif
 *   accordCollectifMajoritaire      ← aiData.pdvRccDetail.pdvRccAccordMajoritaire
 *   validationDREETSObtenue         ← aiData.pdvRccDetail.pdvRccValidationDREETS
 *   indemnitesAuMoinsLegales        ← aiData.pdvRccDetail.pdvRccIndemnitesLegales
 *
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — la RCC est un
 * dispositif franco-français créé par l'ord. du 22/09/2017).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PdvRccTypeDispositif } from '../../core/models/pdv-rcc-conformite.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface PdvRccConformitePrefillInput {
  aiData?: Pick<TravailExtractedData, 'pdvRccDetail'> | null;
  workspaceCountry?: string;
}

/** Valeurs autorisées pour le type de dispositif. */
const TYPE_DISPOSITIF_VALEURS: ReadonlySet<PdvRccTypeDispositif> = new Set(['RCC', 'PDV']);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: PdvRccConformitePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum — `typeDispositif`. */
export function computeTypeDispositif(
  input: PdvRccConformitePrefillInput,
): PdvRccTypeDispositif | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.pdvRccDetail?.pdvRccTypeDispositif;
  if (typeof v !== 'string') return null;
  return TYPE_DISPOSITIF_VALEURS.has(v as PdvRccTypeDispositif)
    ? (v as PdvRccTypeDispositif)
    : null;
}

/** Booléen — `accordCollectifMajoritaire`. */
export function computeAccordMajoritaire(
  input: PdvRccConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.pdvRccDetail?.pdvRccAccordMajoritaire;
  if (typeof v !== 'boolean') return null;
  return v;
}

/** Booléen — `validationDREETSObtenue`. */
export function computeValidationDreets(
  input: PdvRccConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.pdvRccDetail?.pdvRccValidationDREETS;
  if (typeof v !== 'boolean') return null;
  return v;
}

/** Booléen — `indemnitesAuMoinsLegales`. */
export function computeIndemnitesLegales(
  input: PdvRccConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.pdvRccDetail?.pdvRccIndemnitesLegales;
  if (typeof v !== 'boolean') return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 4 champs IA du sous-objet `pdvRccDetail`. F-256 : pré-fill IA
 * désormais branché (slot libéré côté backend).
 */
export function computePrefillCount(input: PdvRccConformitePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeTypeDispositif(input) !== null) count++;
  if (computeAccordMajoritaire(input) !== null) count++;
  if (computeValidationDreets(input) !== null) count++;
  if (computeIndemnitesLegales(input) !== null) count++;
  return count;
}

export const PdvRccConformiteSectionPrefillRules = {
  computeTypeDispositif,
  computeAccordMajoritaire,
  computeValidationDreets,
  computeIndemnitesLegales,
  computePrefillCount,
};
