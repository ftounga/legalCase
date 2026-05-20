/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Contestation ARE" (FR).
 * SF-246-21 : branchement de 2 champs depuis `sante_discrimination_detection`
 * (areTypeDecision — mapping vers TypeDecisionContestee, areMontantConteste).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Codes backend (whitelist prompt) : REFUS_INSCRIPTION, RADIATION, SUPPRESSION_ARE,
 *   REDUCTION_ARE, EXCLUSION_TEMPORAIRE, AUTRE.
 */

import { TypeDecisionContestee } from '../../core/models/contestation-are.model';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-246-21 — mapping codes IA (backend) → TypeDecisionContestee (frontend).
 */
const ARE_AI_CODE_TO_TYPE: Readonly<Record<string, TypeDecisionContestee>> = {
  REFUS_INSCRIPTION: 'REFUS_OUVERTURE_DROITS',
  RADIATION:         'RADIATION',
  SUPPRESSION_ARE:   'DUREE_INDEMNITE',
  REDUCTION_ARE:     'MONTANT_INDEMNITE',
  EXCLUSION_TEMPORAIRE: 'DUREE_INDEMNITE',
  AUTRE:             'AUTRE',
};

export interface ContestationArePrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    // SF-246-21 — sante_discrimination_detection
    areTypeDecision?: string | null;
    areMontantConteste?: number | null;
  } | null;
  workspaceCountry?: string;
  /** Date "now" ISO injectable pour les tests. */
  todayIso?: string;
}

function todayOf(input: ContestationArePrefillInput): string {
  return input.todayIso ?? new Date().toISOString().slice(0, 10);
}

export function computeDateNotificationDecision(input: ContestationArePrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
  return v <= todayOf(input) ? v : null;
}

/**
 * SF-246-21 : type de décision ARE mappé depuis le code IA.
 * Retourne null si le code IA n'est pas reconnu.
 */
export function computeAreTypeDecision(input: ContestationArePrefillInput): TypeDecisionContestee | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.areTypeDecision;
  if (typeof v !== 'string' || v.trim().length === 0) return null;
  const code = v.trim().toUpperCase();
  return ARE_AI_CODE_TO_TYPE[code] ?? null;
}

/** SF-246-21 : montant contesté en € (> 0). */
export function computeAreMontantConteste(input: ContestationArePrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.areMontantConteste;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: ContestationArePrefillInput): number {
  let count = 0;
  if (computeDateNotificationDecision(input) !== null) count++;
  if (computeAreTypeDecision(input) !== null) count++;
  if (computeAreMontantConteste(input) !== null) count++;
  return count;
}

export const ContestationAreSectionPrefillRules = {
  computeDateNotificationDecision,
  computeAreTypeDecision,
  computeAreMontantConteste,
  computePrefillCount,
};
