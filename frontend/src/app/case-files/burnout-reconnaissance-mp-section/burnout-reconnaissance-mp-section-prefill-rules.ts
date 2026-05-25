/**
 * SF-212-28 — Helper partagé pour l'outil "Burn-out — reconnaissance MP"
 * (F-DT-64). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 /
 * F-237 / F-246).
 *
 * Le pipeline IA (SF-212-27 backend) extrait un sous-objet `burnout_detail`
 * (4 champs) projeté à plat sur `TravailExtractedData` via Jackson
 * `@JsonUnwrapped`. FRANCE uniquement (`workspaceCountry === 'FRANCE'` —
 * procédure CRRMP strictement française ; la BE reconnaît le burn-out via
 * Fedris dans un cadre distinct).
 *
 *   diagnosticBurnoutPose          ← aiData.burnoutDiagnostic
 *   tauxIPPEstime                  ← aiData.burnoutTauxIpp
 *   surchargeChargeDocumentee      ← aiData.burnoutSurchargeDocumentee
 *   arretsMaladieMultiples         ← aiData.burnoutArretsMaladie
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface BurnoutReconnaissanceMpPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'burnoutDiagnostic'
    | 'burnoutTauxIpp'
    | 'burnoutSurchargeDocumentee'
    | 'burnoutArretsMaladie'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: BurnoutReconnaissanceMpPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Booléen direct — `diagnosticBurnoutPose`. */
export function computeDiagnostic(
  input: BurnoutReconnaissanceMpPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.burnoutDiagnostic;
  return typeof v === 'boolean' ? v : null;
}

/** Entier borné [0, 100] — `tauxIPPEstime`. Hors plage → null. */
export function computeTauxIpp(
  input: BurnoutReconnaissanceMpPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.burnoutTauxIpp;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > 100) return null;
  return v;
}

/** Booléen direct — `surchargeChargeDocumentee`. */
export function computeSurchargeDocumentee(
  input: BurnoutReconnaissanceMpPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.burnoutSurchargeDocumentee;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `arretsMaladieMultiples`. */
export function computeArretsMaladie(
  input: BurnoutReconnaissanceMpPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.burnoutArretsMaladie;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 4 champs IA.
 */
export function computePrefillCount(
  input: BurnoutReconnaissanceMpPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDiagnostic(input) !== null) count++;
  if (computeTauxIpp(input) !== null) count++;
  if (computeSurchargeDocumentee(input) !== null) count++;
  if (computeArretsMaladie(input) !== null) count++;
  return count;
}

export const BurnoutReconnaissanceMpSectionPrefillRules = {
  computeDiagnostic,
  computeTauxIpp,
  computeSurchargeDocumentee,
  computeArretsMaladie,
  computePrefillCount,
};
