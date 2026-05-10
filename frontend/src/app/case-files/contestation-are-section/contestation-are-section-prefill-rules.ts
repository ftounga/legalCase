/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Contestation ARE" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface ContestationArePrefillInput {
  aiData?: { dateLicenciement?: string | null } | null;
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

export function computePrefillCount(input: ContestationArePrefillInput): number {
  let count = 0;
  if (computeDateNotificationDecision(input) !== null) count++;
  return count;
}

export const ContestationAreSectionPrefillRules = {
  computeDateNotificationDecision,
  computePrefillCount,
};
