/**
 * SF-216-30 — Helper partagé pour l'outil "Donation-partage"
 * (F-FA-DONATION-PARTAGE). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 4 champs pré-remplissables :
 *   - `nombreDescendants`                  ← `nbDescendantsDetecte`
 *   - `respectQuotiteDisponible`           ← `respectQuotiteDisponibleDetected`
 *   - `presencePetitsEnfantsParSubstitution` ← `presencePetitsEnfantsSubstitutionDetectee`
 *   - `donationPartageConjonctive`         ← `donationPartageConjonctiveDetectee`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 1075 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface DonationPartageFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'nbDescendantsDetecte'
        | 'respectQuotiteDisponibleDetected'
        | 'presencePetitsEnfantsSubstitutionDetectee'
        | 'donationPartageConjonctiveDetectee'
      >
    | null;
  workspaceCountry?: string;
}

export interface DonationPartagePrefilledValues {
  nombreDescendants: number | null;
  respectQuotiteDisponible: boolean | null;
  presencePetitsEnfantsParSubstitution: boolean | null;
  donationPartageConjonctive: boolean | null;
}

function positiveIntOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  const n = Math.trunc(value);
  return n >= 1 ? n : null;
}

/**
 * Retourne les valeurs pré-remplies depuis l'IA si disponibles. Hors
 * France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: DonationPartageFrPrefillInput,
): DonationPartagePrefilledValues {
  const empty: DonationPartagePrefilledValues = {
    nombreDescendants: null,
    respectQuotiteDisponible: null,
    presencePetitsEnfantsParSubstitution: null,
    donationPartageConjonctive: null,
  };
  if (input.workspaceCountry !== 'FRANCE') return empty;
  const ai = input.aiData;
  if (!ai) return empty;
  return {
    nombreDescendants: positiveIntOrNull(ai.nbDescendantsDetecte),
    respectQuotiteDisponible:
      typeof ai.respectQuotiteDisponibleDetected === 'boolean'
        ? ai.respectQuotiteDisponibleDetected
        : null,
    presencePetitsEnfantsParSubstitution:
      typeof ai.presencePetitsEnfantsSubstitutionDetectee === 'boolean'
        ? ai.presencePetitsEnfantsSubstitutionDetectee
        : null,
    donationPartageConjonctive:
      typeof ai.donationPartageConjonctiveDetectee === 'boolean'
        ? ai.donationPartageConjonctiveDetectee
        : null,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: DonationPartageFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.nombreDescendants !== null) count += 1;
  if (v.respectQuotiteDisponible !== null) count += 1;
  if (v.presencePetitsEnfantsParSubstitution !== null) count += 1;
  if (v.donationPartageConjonctive !== null) count += 1;
  return count;
}

export const DonationPartageFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
