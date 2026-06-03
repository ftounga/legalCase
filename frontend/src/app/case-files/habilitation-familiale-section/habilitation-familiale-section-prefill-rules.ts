/**
 * SF-222-03 — Helper partagé pour l'outil "Habilitation familiale"
 * (F-FA-HABILITATION-FAMILIALE). Module pur — runtime (`prefillFromAi()`) et
 * static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-222-03 backend extrait depuis `habilitation_familiale_detection` :
 *  - `alteration_facultes_medicalement_constatee` → hfAlteration
 *  - `lien_familial_eligible`                     → hfLienFamilial
 *  - `consensus_familial`                         → hfConsensus
 *  - `besoin_actes_patrimoniaux`                  → hfActesPatrimoniaux
 *  - `besoin_actes_personnels`                    → hfActesPersonnels
 *  - `protection_ponctuelle_ou_generale`          → hfEtendue
 *  - `detecte`                                    → habilitationFamilialeDetectee (flag CONTEXTUAL, non remplissable)
 *
 * Pré-fill des 6 critères saisissables (F-246) :
 *   alterationFacultesMedicalementConstatee ← aiData.hfAlteration
 *   lienFamilialEligible                    ← aiData.hfLienFamilial
 *   consensusFamilial                       ← aiData.hfConsensus
 *   besoinActesPatrimoniaux                 ← aiData.hfActesPatrimoniaux
 *   besoinActesPersonnels                   ← aiData.hfActesPersonnels
 *   protectionPonctuelleOuGenerale          ← aiData.hfEtendue
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 494-1 et s. Cciv).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  EtendueHabilitation,
  LienFamilialHabilitation,
} from '../../core/models/habilitation-familiale.model';

const LIEN_WHITELIST: LienFamilialHabilitation[] = [
  'ASCENDANT', 'DESCENDANT', 'FRERE_SOEUR', 'CONJOINT_PARTENAIRE', 'AUTRE',
];
const ETENDUE_WHITELIST: EtendueHabilitation[] = ['PONCTUELLE', 'GENERALE'];

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface HabilitationFamilialePrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'hfAlteration'
    | 'hfLienFamilial'
    | 'hfConsensus'
    | 'hfActesPatrimoniaux'
    | 'hfActesPersonnels'
    | 'hfEtendue'
  > | null;
  workspaceCountry?: string;
}

function isFrance(input: HabilitationFamilialePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function boolOrNull(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Altération des facultés médicalement constatée. */
export function computeAlteration(input: HabilitationFamilialePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.hfAlteration);
}

/** Consensus familial. */
export function computeConsensus(input: HabilitationFamilialePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.hfConsensus);
}

/** Besoin d'actes patrimoniaux. */
export function computeActesPatrimoniaux(input: HabilitationFamilialePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.hfActesPatrimoniaux);
}

/** Besoin d'actes relatifs à la personne. */
export function computeActesPersonnels(input: HabilitationFamilialePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.hfActesPersonnels);
}

/** Lien familial éligible (enum validé contre la whitelist). */
export function computeLienFamilial(input: HabilitationFamilialePrefillInput): LienFamilialHabilitation | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.hfLienFamilial;
  return typeof v === 'string' && (LIEN_WHITELIST as string[]).includes(v)
    ? (v as LienFamilialHabilitation)
    : null;
}

/** Étendue de la protection (enum validé contre la whitelist). */
export function computeEtendue(input: HabilitationFamilialePrefillInput): EtendueHabilitation | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.hfEtendue;
  return typeof v === 'string' && (ETENDUE_WHITELIST as string[]).includes(v)
    ? (v as EtendueHabilitation)
    : null;
}

/**
 * Nombre exact de critères pré-remplissables — parité stricte avec
 * `prefillFromAi()`. Retourne 0 hors FRANCE.
 * Comptés : alteration, lienFamilial, consensus, actesPatrimoniaux, actesPersonnels, etendue (6).
 */
export function computePrefillCount(input: HabilitationFamilialePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAlteration(input) !== null) count++;
  if (computeLienFamilial(input) !== null) count++;
  if (computeConsensus(input) !== null) count++;
  if (computeActesPatrimoniaux(input) !== null) count++;
  if (computeActesPersonnels(input) !== null) count++;
  if (computeEtendue(input) !== null) count++;
  return count;
}

export const HabilitationFamilialePrefillRules = {
  computeAlteration,
  computeConsensus,
  computeActesPatrimoniaux,
  computeActesPersonnels,
  computeLienFamilial,
  computeEtendue,
  computePrefillCount,
};
