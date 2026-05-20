/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Motif grave" (BE).
 * SF-246-23 — Ajout des 2 dates manquantes : dateConnaissanceFait +
 * dateNotificationMotifs (sources backend réelles depuis travail_be_detection).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Gating : workspaceCountry !== 'BELGIQUE' → null (outil mono-pays BE).
 *
 * Logique miroir :
 *   `dateNotificationRupture ← aiData.dateLicenciement`
 *   `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`
 *   `dateConnaissanceFait    ← aiData.dateConnaissanceFait`     // SF-246-23 BELGIQUE
 *   `dateNotificationMotifs  ← aiData.dateNotificationMotifs`   // SF-246-23 BELGIQUE
 */

export interface MotifGraveBePrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    salaireBrutMensuel?: number | null;
    /** SF-246-23 BELGIQUE — source backend réelle depuis travail_be_detection. */
    dateConnaissanceFait?: string | null;
    /** SF-246-23 BELGIQUE — source backend réelle depuis travail_be_detection. */
    dateNotificationMotifs?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeDateNotificationRupture(input: MotifGraveBePrefillInput): string | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeSalaireMensuelReference(input: MotifGraveBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * SF-246-23 BELGIQUE — date de connaissance du fait constituant le motif grave.
 * Point de départ du délai de 3 j ouvrables art. 35 Loi 03/07/1978.
 */
export function computeDateConnaissanceFait(input: MotifGraveBePrefillInput): string | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.dateConnaissanceFait;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/**
 * SF-246-23 BELGIQUE — date de notification des motifs au travailleur.
 * Point d'arrivée du 2e délai de 3 j ouvrables.
 */
export function computeDateNotificationMotifs(input: MotifGraveBePrefillInput): string | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.dateNotificationMotifs;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: MotifGraveBePrefillInput): number {
  let count = 0;
  if (computeDateNotificationRupture(input) !== null) count++;
  if (computeSalaireMensuelReference(input) !== null) count++;
  if (computeDateConnaissanceFait(input) !== null) count++;     // SF-246-23
  if (computeDateNotificationMotifs(input) !== null) count++;   // SF-246-23
  return count;
}

export const MotifGraveBeSectionPrefillRules = {
  computeDateNotificationRupture,
  computeSalaireMensuelReference,
  computeDateConnaissanceFait,       // SF-246-23
  computeDateNotificationMotifs,     // SF-246-23
  computePrefillCount,
};
