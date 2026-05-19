/**
 * F-236 SF-236-02 — Helper partagé `IndivisionPrefillRules`.
 * 2 champs : dateOrigineIndivision (dateSeparation IA, ISO),
 * occupationBienParUnIndivisaire (compté uniquement si logementCommunDetected === true).
 *
 * SF-246-08 : `dateSeparation` désormais champ réel de `FamilleExtractedData`
 * (sous-objet backend `vie_commune_detection`). Suppression de l'intersection
 * aspirationnelle. Ajout de la garde BE : retourne 0 hors France.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface IndivisionPrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateOrigine(input: IndivisionPrefillInput): string | null {
  const v = input.aiData?.dateSeparation;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** Compté uniquement si l'IA détecte logementCommun = true (pas false). */
export function isOccupationBien(input: IndivisionPrefillInput): boolean {
  return input.aiData?.logementCommunDetected === true;
}

export function computePrefillCount(input: IndivisionPrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
  let n = 0;
  if (computeDateOrigine(input) !== null) n++;
  if (isOccupationBien(input)) n++;
  return n;
}

export const IndivisionPrefillRules = {
  ISO_DATE_REGEX,
  computeDateOrigine,
  isOccupationBien,
  computePrefillCount,
};
