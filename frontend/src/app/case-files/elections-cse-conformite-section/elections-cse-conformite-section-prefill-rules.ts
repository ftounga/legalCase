/**
 * SF-212-32 — Helper partagé pour l'outil "Élections CSE — conformité"
 * (F-DT-65). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 /
 * F-237 / F-246).
 *
 * Le pipeline IA (SF-212-31 backend) extrait un sous-objet
 * `elections_cse_detail` (4 champs) projeté à plat sur `TravailExtractedData`
 * via Jackson `@JsonUnwrapped`. FRANCE uniquement (`workspaceCountry ===
 * 'FRANCE'` — procédure CSE strictement française ; la BE dispose d'un
 * régime distinct conseil d'entreprise + délégation syndicale).
 *
 *   dateElection                ← aiData.electionCseDateElection (ISO YYYY-MM-DD)
 *   papNegocieAvecOS            ← aiData.electionCsePapNegocie
 *   collegesConformes           ← aiData.electionCseCollegesConformes
 *   resultatsContestes          ← aiData.electionCseResultatsContestes
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ElectionsCseConformitePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'electionCseDateElection'
    | 'electionCsePapNegocie'
    | 'electionCseCollegesConformes'
    | 'electionCseResultatsContestes'
  > | null;
  workspaceCountry?: string;
}

/** Format ISO YYYY-MM-DD strict (validation de surface). */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ElectionsCseConformitePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Date ISO YYYY-MM-DD — `dateElection`. Format invalide → null. */
export function computeDateElection(
  input: ElectionsCseConformitePrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.electionCseDateElection;
  if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
  return v;
}

/** Booléen direct — `papNegocieAvecOS`. */
export function computePapNegocie(
  input: ElectionsCseConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.electionCsePapNegocie;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `collegesConformes`. */
export function computeCollegesConformes(
  input: ElectionsCseConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.electionCseCollegesConformes;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `resultatsContestes`. */
export function computeResultatsContestes(
  input: ElectionsCseConformitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.electionCseResultatsContestes;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 4 champs IA.
 */
export function computePrefillCount(
  input: ElectionsCseConformitePrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDateElection(input) !== null) count++;
  if (computePapNegocie(input) !== null) count++;
  if (computeCollegesConformes(input) !== null) count++;
  if (computeResultatsContestes(input) !== null) count++;
  return count;
}

export const ElectionsCseConformiteSectionPrefillRules = {
  computeDateElection,
  computePapNegocie,
  computeCollegesConformes,
  computeResultatsContestes,
  computePrefillCount,
};
