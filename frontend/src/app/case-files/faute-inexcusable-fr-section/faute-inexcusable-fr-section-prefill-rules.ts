/**
 * SF-212-10 — Helper partagé pour l'outil "Faute inexcusable de l'employeur"
 * (F-DT-91). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-09 backend) extrait un sous-objet
 * `faute_inexcusable_detail` (4 champs) projeté à plat sur
 * `TravailExcusableExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — L. 452-1 à L. 452-5 CSS ; Cass. ass.
 * plén. 24/06/2005 ; en BE, régimes faute grave / intentionnelle distincts).
 *
 *   conscienceDangerEmployeurEtablie    ← aiData.fauteInexcusableConscienceDanger
 *   signalementDangerPrior              ← aiData.fauteInexcusableSignalementPrior
 *   mesuresPreventionPrises             ← aiData.fauteInexcusableMesuresPrevention
 *   tauxIpp                             ← aiData.fauteInexcusableTauxIpp
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface FauteInexcusableFrPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'fauteInexcusableConscienceDanger'
    | 'fauteInexcusableSignalementPrior'
    | 'fauteInexcusableMesuresPrevention'
    | 'fauteInexcusableTauxIpp'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: FauteInexcusableFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Booléen direct — `conscienceDangerEmployeurEtablie`. */
export function computeConscienceDanger(
  input: FauteInexcusableFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteInexcusableConscienceDanger;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `signalementDangerPrior`. */
export function computeSignalementPrior(
  input: FauteInexcusableFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteInexcusableSignalementPrior;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `mesuresPreventionPrises`. */
export function computeMesuresPrevention(
  input: FauteInexcusableFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteInexcusableMesuresPrevention;
  return typeof v === 'boolean' ? v : null;
}

/** Taux IPP entier borné [0, 100] — `tauxIpp`. */
export function computeTauxIpp(
  input: FauteInexcusableFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.fauteInexcusableTauxIpp;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > 100) return null;
  return Math.trunc(v);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 4 champs IA.
 */
export function computePrefillCount(input: FauteInexcusableFrPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeConscienceDanger(input) !== null) count++;
  if (computeSignalementPrior(input) !== null) count++;
  if (computeMesuresPrevention(input) !== null) count++;
  if (computeTauxIpp(input) !== null) count++;
  return count;
}

export const FauteInexcusableFrSectionPrefillRules = {
  computeConscienceDanger,
  computeSignalementPrior,
  computeMesuresPrevention,
  computeTauxIpp,
  computePrefillCount,
};
