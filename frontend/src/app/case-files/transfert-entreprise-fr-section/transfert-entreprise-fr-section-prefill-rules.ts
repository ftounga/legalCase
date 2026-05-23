/**
 * SF-212-06 — Helper partagé pour l'outil "Transfert d'entreprise —
 * L. 1224-1" (F-DT-72). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-05 backend) extrait un sous-objet
 * `transfert_entreprise_detail` (5 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'`
 *  — L. 1224-1 CT, Cass. soc. 18/07/2000 n°98-46.071 ; en BE, le maintien
 * relève de la CCT 32bis, régime distinct).
 *
 *   typeTransfert                       ← aiData.transfertTypeTransfert
 *   eeaIdentifieeAvantTransfert         ← aiData.transfertEeaIdentifiee
 *   activiteEconomiquePreservee         ← aiData.transfertActivitePreservee
 *   licenciementsPreTransfert           ← aiData.transfertLicenciementsPreTransfert
 *   dateTransfert                       ← aiData.transfertDateTransfert
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TransfertEntrepriseFrTypeTransfert } from '../../core/models/transfert-entreprise-fr.model';

/** Whitelist stricte des types de transfert acceptés (alignée backend). */
const TYPES_TRANSFERT_AUTORISES: ReadonlySet<string> = new Set([
  'CESSION',
  'FUSION',
  'APPORT_PARTIEL_ACTIF',
  'EXTERNALISATION',
  'REPRISE_ACTIVITE',
  'AUTRE',
]);

/** Regex ISO YYYY-MM-DD strict. */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface TransfertEntrepriseFrPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'transfertTypeTransfert'
    | 'transfertEeaIdentifiee'
    | 'transfertActivitePreservee'
    | 'transfertLicenciementsPreTransfert'
    | 'transfertDateTransfert'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: TransfertEntrepriseFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum strict 6 valeurs — `typeTransfert`. */
export function computeTypeTransfert(
  input: TransfertEntrepriseFrPrefillInput,
): TransfertEntrepriseFrTypeTransfert | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.transfertTypeTransfert;
  if (typeof v !== 'string' || !TYPES_TRANSFERT_AUTORISES.has(v)) return null;
  return v as TransfertEntrepriseFrTypeTransfert;
}

/** Booléen direct — `eeaIdentifieeAvantTransfert`. */
export function computeEeaIdentifiee(
  input: TransfertEntrepriseFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.transfertEeaIdentifiee;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `activiteEconomiquePreservee`. */
export function computeActivitePreservee(
  input: TransfertEntrepriseFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.transfertActivitePreservee;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `licenciementsPreTransfert`. */
export function computeLicenciementsPreTransfert(
  input: TransfertEntrepriseFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.transfertLicenciementsPreTransfert;
  return typeof v === 'boolean' ? v : null;
}

/** Date ISO YYYY-MM-DD — `dateTransfert`. */
export function computeDateTransfert(
  input: TransfertEntrepriseFrPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.transfertDateTransfert;
  if (typeof v !== 'string' || !ISO_DATE_REGEX.test(v)) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 5 champs IA.
 */
export function computePrefillCount(input: TransfertEntrepriseFrPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeTypeTransfert(input) !== null) count++;
  if (computeEeaIdentifiee(input) !== null) count++;
  if (computeActivitePreservee(input) !== null) count++;
  if (computeLicenciementsPreTransfert(input) !== null) count++;
  if (computeDateTransfert(input) !== null) count++;
  return count;
}

export const TransfertEntrepriseFrSectionPrefillRules = {
  computeTypeTransfert,
  computeEeaIdentifiee,
  computeActivitePreservee,
  computeLicenciementsPreTransfert,
  computeDateTransfert,
  computePrefillCount,
};
