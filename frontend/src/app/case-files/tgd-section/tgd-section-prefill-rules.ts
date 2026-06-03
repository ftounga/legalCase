/**
 * SF-222-02 — Helper partagé pour l'outil "Téléphone Grave Danger —
 * éligibilité" (F-FA-TGD). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-222-02 backend extrait depuis `tgd_detection` :
 *  - `danger_grave`                          → tgdDangerGrave
 *  - `violences_averees_ou_vraisemblables`   → tgdViolences
 *  - `interdiction_contact_procedure`        → tgdInterdictionContact
 *  - `non_cohabitation`                      → tgdNonCohabitation
 *  - `consentement_victime`                  → tgdConsentement
 *  - `detecte`                               → tgdDetecte (flag CONTEXTUAL, non remplissable)
 *
 * Pré-fill des 5 critères saisissables (F-246) :
 *   dangerGrave                        ← aiData.tgdDangerGrave
 *   violencesAvereesOuVraisemblables   ← aiData.tgdViolences
 *   interdictionContactProcedure       ← aiData.tgdInterdictionContact
 *   nonCohabitation                    ← aiData.tgdNonCohabitation
 *   consentementVictime                ← aiData.tgdConsentement
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 41-3-1 CPP).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface TgdPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'tgdDangerGrave'
    | 'tgdViolences'
    | 'tgdInterdictionContact'
    | 'tgdNonCohabitation'
    | 'tgdConsentement'
  > | null;
  workspaceCountry?: string;
}

function isFrance(input: TgdPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function boolOrNull(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Danger grave caractérisé. */
export function computeDangerGrave(input: TgdPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.tgdDangerGrave);
}

/** Violences avérées ou vraisemblables. */
export function computeViolences(input: TgdPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.tgdViolences);
}

/** Interdiction de contact issue d'une procédure. */
export function computeInterdictionContact(input: TgdPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.tgdInterdictionContact);
}

/** Non-cohabitation auteur / victime. */
export function computeNonCohabitation(input: TgdPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.tgdNonCohabitation);
}

/** Consentement exprès de la victime. */
export function computeConsentement(input: TgdPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return boolOrNull(input.aiData?.tgdConsentement);
}

/**
 * Nombre exact de critères pré-remplissables — parité stricte avec
 * `prefillFromAi()`. Retourne 0 hors FRANCE.
 * Comptés : dangerGrave, violences, interdictionContact, nonCohabitation, consentement (5).
 */
export function computePrefillCount(input: TgdPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDangerGrave(input) !== null) count++;
  if (computeViolences(input) !== null) count++;
  if (computeInterdictionContact(input) !== null) count++;
  if (computeNonCohabitation(input) !== null) count++;
  if (computeConsentement(input) !== null) count++;
  return count;
}

export const TgdPrefillRules = {
  computeDangerGrave,
  computeViolences,
  computeInterdictionContact,
  computeNonCohabitation,
  computeConsentement,
  computePrefillCount,
};
