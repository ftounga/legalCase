/**
 * SF-212-08 — Helper partagé pour l'outil "CSP/CRP — conformité de la
 * proposition" (F-DT-44). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-07 backend) extrait un sous-objet `csp_detail`
 * (6 champs) projeté à plat sur `TravailExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — le CSP L. 1233-65+ CT est strictement
 * français pour les entreprises < 1 000 salariés).
 *
 *   effectifEntreprise          ← aiData.cspEffectifEntreprise
 *   cspPropose                  ← aiData.cspProposeDetail
 *   documentInformationRemis    ← aiData.cspDocumentRemis
 *   dateRemise                  ← aiData.cspDateRemise
 *   adhesionSalarie             ← aiData.cspAdhesion
 *   salaireMensuelBrutEuros     ← aiData.cspSalaireMensuelBrut
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface CspCrpFrPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'cspEffectifEntreprise'
    | 'cspProposeDetail'
    | 'cspDocumentRemis'
    | 'cspDateRemise'
    | 'cspAdhesion'
    | 'cspSalaireMensuelBrut'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: CspCrpFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Entier borné [0, 100 000] — `effectifEntreprise`. */
export function computeEffectifEntreprise(
  input: CspCrpFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspEffectifEntreprise;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0 || v > 100_000) {
    return null;
  }
  return Math.trunc(v);
}

/** Booléen direct — `cspPropose`. */
export function computeCspPropose(
  input: CspCrpFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspProposeDetail;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `documentInformationRemis`. */
export function computeDocumentInformationRemis(
  input: CspCrpFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspDocumentRemis;
  return typeof v === 'boolean' ? v : null;
}

/** Date ISO YYYY-MM-DD — `dateRemise`. */
export function computeDateRemise(
  input: CspCrpFrPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspDateRemise;
  if (typeof v !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  return v;
}

/** Booléen tri-état — `adhesionSalarie` (null = inconnu). */
export function computeAdhesionSalarie(
  input: CspCrpFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspAdhesion;
  return typeof v === 'boolean' ? v : null;
}

/** Salaire mensuel brut strictement positif — `salaireMensuelBrutEuros`. */
export function computeSalaireMensuelBrutEuros(
  input: CspCrpFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cspSalaireMensuelBrut;
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 6 champs IA.
 */
export function computePrefillCount(input: CspCrpFrPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeEffectifEntreprise(input) !== null) count++;
  if (computeCspPropose(input) !== null) count++;
  if (computeDocumentInformationRemis(input) !== null) count++;
  if (computeDateRemise(input) !== null) count++;
  if (computeAdhesionSalarie(input) !== null) count++;
  if (computeSalaireMensuelBrutEuros(input) !== null) count++;
  return count;
}

export const CspCrpFrSectionPrefillRules = {
  computeEffectifEntreprise,
  computeCspPropose,
  computeDocumentInformationRemis,
  computeDateRemise,
  computeAdhesionSalarie,
  computeSalaireMensuelBrutEuros,
  computePrefillCount,
};
