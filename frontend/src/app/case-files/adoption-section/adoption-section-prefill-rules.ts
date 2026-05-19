/**
 * F-236 SF-236-02 — Helper partagé `AdoptionPrefillRules`.
 *
 * 5 champs : formeAdoption ('PLENIERE'|'SIMPLE'), pupilleEtat (bool — compté
 * uniquement si true, runtime pose IA seulement sur true), adoptantMarie
 * (idem), ageAdoptant (number >= 0), ageAdopte (number >= 0).
 *
 * SF-246-09 : ageAdoptantDetecte et ageAdopteDetecte sont désormais des champs
 * réels de FamilleExtractedData (source backend `filiation_detection`) — les
 * types d'intersection aspirationnels sont supprimés, lecture directe depuis
 * le record.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export type FormeAdoption = 'PLENIERE' | 'SIMPLE';

type Ai = Partial<FamilleExtractedData>;

export interface AdoptionPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeFormeAdoption(input: AdoptionPrefillInput): FormeAdoption | null {
  const v = input.aiData?.formeAdoptionDemandeeDetected;
  return v === 'PLENIERE' || v === 'SIMPLE' ? v : null;
}

/** Compté UNIQUEMENT si detected === true (parité avec runtime). */
export function isPupilleEtatTrue(input: AdoptionPrefillInput): boolean {
  return input.aiData?.pupilleEtatDetected === true;
}

export function isAdoptantMarieTrue(input: AdoptionPrefillInput): boolean {
  return input.aiData?.adoptantMarieDetected === true;
}

export function computeAgeAdoptant(input: AdoptionPrefillInput): number | null {
  const v = input.aiData?.ageAdoptantDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computeAgeAdopte(input: AdoptionPrefillInput): number | null {
  const v = input.aiData?.ageAdopteDetecte;
  return typeof v === 'number' && v >= 0 ? v : null;
}

export function computePrefillCount(input: AdoptionPrefillInput): number {
  let n = 0;
  if (computeFormeAdoption(input) !== null) n++;
  if (isPupilleEtatTrue(input)) n++;
  if (isAdoptantMarieTrue(input)) n++;
  if (computeAgeAdoptant(input) !== null) n++;
  if (computeAgeAdopte(input) !== null) n++;
  return n;
}

export const AdoptionPrefillRules = {
  computeFormeAdoption,
  isPupilleEtatTrue,
  isAdoptantMarieTrue,
  computeAgeAdoptant,
  computeAgeAdopte,
  computePrefillCount,
};
