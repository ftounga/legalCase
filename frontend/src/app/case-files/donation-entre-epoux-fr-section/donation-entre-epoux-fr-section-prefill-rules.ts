/**
 * SF-216-24 — Helper partagé pour l'outil "Donation entre époux"
 * (F-FA-DONATION-ENTRE-EPOUX). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 5 champs pré-remplissables :
 *   - `regimeMatrimonial`           ← `regimeMatrimonialDetecte`
 *   - `clauseAttributionIntegrale`  ← `clauseAttributionIntegraleDetected`
 *   - `enfantsNonCommuns`           ← `enfantsNonCommunsDetected`
 *   - `revocabiliteDetectee`        ← `revocabiliteDetectee`
 *   - `bienDonneType`               ← `bienDonnePrincipalType`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 1096 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  BienDonneType,
  RegimeMatrimonial,
} from '../../core/models/donation-entre-epoux-fr.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface DonationEntreEpouxFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'regimeMatrimonialDetecte'
        | 'clauseAttributionIntegraleDetected'
        | 'enfantsNonCommunsDetected'
        | 'revocabiliteDetectee'
        | 'bienDonnePrincipalType'
      >
    | null;
  workspaceCountry?: string;
}

export interface DonationEntreEpouxPrefilledValues {
  regimeMatrimonial: RegimeMatrimonial | null;
  clauseAttributionIntegrale: boolean | null;
  enfantsNonCommuns: boolean | null;
  revocabiliteDetectee: boolean | null;
  bienDonneType: BienDonneType | null;
}

const REGIME_VALUES: ReadonlyArray<RegimeMatrimonial> = [
  'COMMUNAUTE_LEGALE',
  'COMMUNAUTE_UNIVERSELLE',
  'SEPARATION_DE_BIENS',
  'PARTICIPATION_AUX_ACQUETS',
  'AUTRE',
];

const BIEN_VALUES: ReadonlyArray<BienDonneType> = [
  'IMMOBILIER',
  'MOBILIER',
  'PORTEFEUILLE',
  'NUMERAIRE',
  'AUTRE',
];

function regimeOrNull(value: unknown): RegimeMatrimonial | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  return (REGIME_VALUES as ReadonlyArray<string>).includes(v)
    ? (v as RegimeMatrimonial)
    : null;
}

function bienOrNull(value: unknown): BienDonneType | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  return (BIEN_VALUES as ReadonlyArray<string>).includes(v)
    ? (v as BienDonneType)
    : null;
}

/**
 * Retourne les valeurs pré-remplies depuis l'IA si disponibles. Hors
 * France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: DonationEntreEpouxFrPrefillInput,
): DonationEntreEpouxPrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return {
      regimeMatrimonial: null,
      clauseAttributionIntegrale: null,
      enfantsNonCommuns: null,
      revocabiliteDetectee: null,
      bienDonneType: null,
    };
  }
  const ai = input.aiData;
  if (!ai) {
    return {
      regimeMatrimonial: null,
      clauseAttributionIntegrale: null,
      enfantsNonCommuns: null,
      revocabiliteDetectee: null,
      bienDonneType: null,
    };
  }
  return {
    regimeMatrimonial: regimeOrNull(ai.regimeMatrimonialDetecte),
    clauseAttributionIntegrale:
      typeof ai.clauseAttributionIntegraleDetected === 'boolean'
        ? ai.clauseAttributionIntegraleDetected
        : null,
    enfantsNonCommuns:
      typeof ai.enfantsNonCommunsDetected === 'boolean'
        ? ai.enfantsNonCommunsDetected
        : null,
    revocabiliteDetectee:
      typeof ai.revocabiliteDetectee === 'boolean'
        ? ai.revocabiliteDetectee
        : null,
    bienDonneType: bienOrNull(ai.bienDonnePrincipalType),
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: DonationEntreEpouxFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.regimeMatrimonial !== null) count += 1;
  if (v.clauseAttributionIntegrale !== null) count += 1;
  if (v.enfantsNonCommuns !== null) count += 1;
  if (v.revocabiliteDetectee !== null) count += 1;
  if (v.bienDonneType !== null) count += 1;
  return count;
}

export const DonationEntreEpouxFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
