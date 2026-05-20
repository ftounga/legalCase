/**
 * SF-212-02 — Helper partagé pour l'outil "Licenciement pour faute grave /
 * faute lourde" (F-DT-36). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-01 backend) extrait un sous-objet `faute_grave_detail`
 * (6 champs) projeté à plat sur `TravailExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — la distinction faute grave / faute lourde
 * est strictement française, L.1234-1 s. CT ; L.1234-9 CT pour la faute
 * lourde — Cass. soc. 18/06/2013 n°11-14.393).
 *
 *   faitsReproches               ← aiData.fauteGraveFaitsReproches
 *   datesFaits                   ← aiData.fauteGraveDatesFaits
 *   qualificationEmployeur       ← aiData.fauteGraveQualificationEmployeur
 *   intentionNuireAlleeguee      ← aiData.fauteGraveIntentionNuireAlleeguee
 *   ancienneteMois               ← aiData.fauteGraveAncienneteMois
 *   salaireMensuelBrutEuros      ← aiData.fauteGraveSalaireMensuelBrut
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { LicenciementFauteGraveLourdQualification } from '../../core/models/licenciement-faute-grave-lourd.model';

/** Whitelist stricte des qualifications acceptées (alignée backend). */
const QUALIFICATIONS_AUTORISEES: ReadonlySet<string> = new Set([
  'FAUTE_SIMPLE',
  'FAUTE_GRAVE',
  'FAUTE_LOURDE',
]);

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface LicenciementFauteGraveLourdPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'fauteGraveFaitsReproches'
    | 'fauteGraveDatesFaits'
    | 'fauteGraveQualificationEmployeur'
    | 'fauteGraveIntentionNuireAlleeguee'
    | 'fauteGraveAncienneteMois'
    | 'fauteGraveSalaireMensuelBrut'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: LicenciementFauteGraveLourdPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Texte non vide — `faitsReproches`. */
export function computeFaitsReproches(
  input: LicenciementFauteGraveLourdPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveFaitsReproches;
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  return trimmed.length > 0 ? trimmed : null;
}

/** Tableau de dates ISO YYYY-MM-DD — `datesFaits`. Null si pas de tableau ou tableau vide. */
export function computeDatesFaits(
  input: LicenciementFauteGraveLourdPrefillInput,
): string[] | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveDatesFaits;
  if (!Array.isArray(v) || v.length === 0) return null;
  const cleaned = v.filter((d): d is string => typeof d === 'string' && d.trim().length > 0);
  return cleaned.length > 0 ? cleaned : null;
}

/** Enum strict 3 valeurs — `qualificationEmployeur`. */
export function computeQualificationEmployeur(
  input: LicenciementFauteGraveLourdPrefillInput,
): LicenciementFauteGraveLourdQualification | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveQualificationEmployeur;
  if (typeof v !== 'string' || !QUALIFICATIONS_AUTORISEES.has(v)) return null;
  return v as LicenciementFauteGraveLourdQualification;
}

/** Booléen direct — `intentionNuireAlleeguee` (peut rester null si IA non tranché). */
export function computeIntentionNuireAlleeguee(
  input: LicenciementFauteGraveLourdPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveIntentionNuireAlleeguee;
  return typeof v === 'boolean' ? v : null;
}

/** Entier borné [0, 600] mois — `ancienneteMois`. */
export function computeAncienneteMois(
  input: LicenciementFauteGraveLourdPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveAncienneteMois;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0 || v > 600) return null;
  return Math.trunc(v);
}

/** Décimal > 0 — `salaireMensuelBrutEuros`. */
export function computeSalaireMensuelBrut(
  input: LicenciementFauteGraveLourdPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteGraveSalaireMensuelBrut;
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 6 champs IA.
 */
export function computePrefillCount(input: LicenciementFauteGraveLourdPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeFaitsReproches(input) !== null) count++;
  if (computeDatesFaits(input) !== null) count++;
  if (computeQualificationEmployeur(input) !== null) count++;
  if (computeIntentionNuireAlleeguee(input) !== null) count++;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeSalaireMensuelBrut(input) !== null) count++;
  return count;
}

export const LicenciementFauteGraveLourdSectionPrefillRules = {
  computeFaitsReproches,
  computeDatesFaits,
  computeQualificationEmployeur,
  computeIntentionNuireAlleeguee,
  computeAncienneteMois,
  computeSalaireMensuelBrut,
  computePrefillCount,
};
