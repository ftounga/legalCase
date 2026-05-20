/**
 * SF-206-02 — Helper partagé pour l'outil "Abandon de poste / présomption de
 * démission" (F-DT-42). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA (SF-206-01 backend) extrait un sous-objet
 * `abandon_poste_detail` (8 champs) projeté à plat sur `TravailExtractedData`.
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — mécanisme franco-
 * français de la loi 21/12/2022, art. L.1237-1-1 CT).
 *
 *   dateMiseEnDemeurePresentee    ← aiData.abandonPosteDateMiseEnDemeure (ISO)
 *   modeNotificationMiseEnDemeure ← aiData.abandonPosteModeNotification (enum)
 *   delaiAccordeJours             ← aiData.abandonPosteDelaiAccordeJours (int)
 *   motifAbsenceInvoque           ← aiData.abandonPosteMotifAbsence (enum)
 *   dateRepriseOuJustification    ← aiData.abandonPosteDateReprise (ISO)
 *   miseEnDemeureMentionneDelai   ← aiData.abandonPosteMedMentionneDelai (bool)
 *   miseEnDemeureMentionneConsequences ← aiData.abandonPosteMedMentionneConsequences (bool)
 *   repriseOuJustificationDansDelai ← aiData.abandonPosteRepriseDansDelai (bool)
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import {
  AbandonPosteModeNotification,
  AbandonPosteMotifAbsence,
} from '../../core/models/abandon-poste-presomption-demission.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const MODE_NOTIFICATION_VALUES: readonly AbandonPosteModeNotification[] = [
  'LRAR',
  'REMISE_MAIN_PROPRE',
  'AUTRE',
];

const MOTIF_ABSENCE_VALUES: readonly AbandonPosteMotifAbsence[] = [
  'AUCUN',
  'MEDICAL',
  'DROIT_RETRAIT',
  'DROIT_GREVE',
  'MODIFICATION_CONTRAT_REFUSEE',
  'DEFAUT_PAIEMENT_SALAIRE',
  'AUTRE',
];

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface AbandonPostePresomptionDemissionPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'abandonPosteDateMiseEnDemeure'
    | 'abandonPosteModeNotification'
    | 'abandonPosteDelaiAccordeJours'
    | 'abandonPosteMotifAbsence'
    | 'abandonPosteDateReprise'
    | 'abandonPosteMedMentionneDelai'
    | 'abandonPosteMedMentionneConsequences'
    | 'abandonPosteRepriseDansDelai'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: AbandonPostePresomptionDemissionPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Normalise une chaîne enum côté IA (toUpperCase + trim) — null si vide / non whitelisté. */
function normalizeEnum<T extends string>(
  raw: string | null | undefined,
  whitelist: readonly T[],
): T | null {
  if (!raw || typeof raw !== 'string') return null;
  const candidate = raw.trim().toUpperCase() as T;
  return whitelist.includes(candidate) ? candidate : null;
}

/** `dateMiseEnDemeurePresentee` — date ISO YYYY-MM-DD. */
export function computeDateMiseEnDemeure(
  input: AbandonPostePresomptionDemissionPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteDateMiseEnDemeure;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `modeNotificationMiseEnDemeure` — enum LRAR | REMISE_MAIN_PROPRE | AUTRE. */
export function computeModeNotification(
  input: AbandonPostePresomptionDemissionPrefillInput,
): AbandonPosteModeNotification | null {
  if (!isFrance(input)) return null;
  return normalizeEnum<AbandonPosteModeNotification>(
    input.aiData?.abandonPosteModeNotification,
    MODE_NOTIFICATION_VALUES,
  );
}

/** `delaiAccordeJours` — entier ≥ 0. */
export function computeDelaiAccordeJours(
  input: AbandonPostePresomptionDemissionPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteDelaiAccordeJours;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return Math.trunc(v);
}

/** `motifAbsenceInvoque` — enum (AUCUN | MEDICAL | …). */
export function computeMotifAbsence(
  input: AbandonPostePresomptionDemissionPrefillInput,
): AbandonPosteMotifAbsence | null {
  if (!isFrance(input)) return null;
  return normalizeEnum<AbandonPosteMotifAbsence>(
    input.aiData?.abandonPosteMotifAbsence,
    MOTIF_ABSENCE_VALUES,
  );
}

/** `dateRepriseOuJustification` — date ISO YYYY-MM-DD. */
export function computeDateReprise(
  input: AbandonPostePresomptionDemissionPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteDateReprise;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `miseEnDemeureMentionneDelai` — booléen factuel direct. */
export function computeMedMentionneDelai(
  input: AbandonPostePresomptionDemissionPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteMedMentionneDelai;
  return typeof v === 'boolean' ? v : null;
}

/** `miseEnDemeureMentionneConsequences` — booléen factuel direct. */
export function computeMedMentionneConsequences(
  input: AbandonPostePresomptionDemissionPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteMedMentionneConsequences;
  return typeof v === 'boolean' ? v : null;
}

/** `repriseOuJustificationDansDelai` — booléen factuel direct. */
export function computeRepriseDansDelai(
  input: AbandonPostePresomptionDemissionPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.abandonPosteRepriseDansDelai;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 */
export function computePrefillCount(
  input: AbandonPostePresomptionDemissionPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDateMiseEnDemeure(input) !== null) count++;
  if (computeModeNotification(input) !== null) count++;
  if (computeDelaiAccordeJours(input) !== null) count++;
  if (computeMotifAbsence(input) !== null) count++;
  if (computeDateReprise(input) !== null) count++;
  if (computeMedMentionneDelai(input) !== null) count++;
  if (computeMedMentionneConsequences(input) !== null) count++;
  if (computeRepriseDansDelai(input) !== null) count++;
  return count;
}

export const AbandonPostePresomptionDemissionSectionPrefillRules = {
  computeDateMiseEnDemeure,
  computeModeNotification,
  computeDelaiAccordeJours,
  computeMotifAbsence,
  computeDateReprise,
  computeMedMentionneDelai,
  computeMedMentionneConsequences,
  computeRepriseDansDelai,
  computePrefillCount,
};
