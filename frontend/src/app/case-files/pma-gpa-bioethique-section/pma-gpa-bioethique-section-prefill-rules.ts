/**
 * F-236 SF-236-02 — Helper partagé `PmaGpaBioethiquePrefillRules`.
 *
 * SF-246-27 : 4 champs pré-fillables :
 *   1. `dispositif` (string enum 3 codes) — depuis `ai.dispositifBioethiqueDetecte`.
 *   2. `datePma` (string ISO YYYY-MM-DD) — depuis `ai.datePmaDetected`.
 *   3. `dateReconnaissanceAnterieure` (string ISO YYYY-MM-DD) — depuis
 *      `ai.dateReconnaissanceAnterieurePmaDetected`.
 *   4. `dateDon` (string ISO YYYY-MM-DD) — depuis `ai.dateDonGametesDetected`.
 *
 * Comptage : 1 pour dispositif (si reconnu ≠ défaut), 1 pour chaque date non-null
 * après validation ISO → maximum 4.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { DispositifBioethique } from '../../core/models/pma-gpa-bioethique.model';

const DEFAULT_DISPOSITIF: DispositifBioethique = 'PMA_RECONNAISSANCE_ANTICIPEE';
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

type Ai = Partial<FamilleExtractedData> & {
  dispositifBioethiqueDetecte?: string | null;
};

export interface PmaGpaBioethiquePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseDispositifFromIa(value: unknown): DispositifBioethique | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (v === 'PMA_RECONNAISSANCE_ANTICIPEE'
      || v === 'GPA_TRANSCRIPTION_ETAT_CIVIL'
      || v === 'DON_GAMETES_ACCES_ORIGINES') {
    return v as DispositifBioethique;
  }
  return null;
}

export function computeDispositif(input: PmaGpaBioethiquePrefillInput): DispositifBioethique | null {
  return parseDispositifFromIa(input.aiData?.dispositifBioethiqueDetecte);
}

/** SF-246-27 : date de début du protocole PMA (art. L2141-2 CSP). */
export function computeDatePma(input: PmaGpaBioethiquePrefillInput): string | null {
  const v = input.aiData?.datePmaDetected;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-27 : date de la reconnaissance anticipée devant notaire (art. L2141-5 al. 2 CSP). */
export function computeDateReconnaissanceAnterieure(input: PmaGpaBioethiquePrefillInput): string | null {
  const v = input.aiData?.dateReconnaissanceAnterieurePmaDetected;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-27 : date du don de gamètes (art. L2143-3 CSP). */
export function computeDateDon(input: PmaGpaBioethiquePrefillInput): string | null {
  const v = input.aiData?.dateDonGametesDetected;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

export function computePrefillCount(input: PmaGpaBioethiquePrefillInput): number {
  let n = 0;
  const d = computeDispositif(input);
  if (d !== null) n++;
  if (computeDatePma(input) !== null) n++;
  if (computeDateReconnaissanceAnterieure(input) !== null) n++;
  if (computeDateDon(input) !== null) n++;
  return n;
}

export const PmaGpaBioethiquePrefillRules = {
  DEFAULT_DISPOSITIF,
  ISO_DATE_RE,
  parseDispositifFromIa,
  computeDispositif,
  computeDatePma,
  computeDateReconnaissanceAnterieure,
  computeDateDon,
  computePrefillCount,
};
