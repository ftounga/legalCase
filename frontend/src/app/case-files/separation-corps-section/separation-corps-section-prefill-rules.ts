/**
 * F-236 SF-236-02 — Helper partagé `SeparationCorpsPrefillRules`.
 * 3 champs : dateJugementSeparationCorps (ISO via dateSeparation),
 * patrimoineCommun (boolean), patrimoineCommunEur (number > 0).
 *
 * SF-246-08 : `dateSeparation` désormais champ réel (`vie_commune_detection`).
 * Ajout de `computePatrimoineCommunEur` (montant € — invariant §5.2 > 0 ou null,
 * distinct du boolean `patrimoineCommun` existant).
 * Ajout de la garde BE : retourne 0 hors France.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface SeparationCorpsPrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateJugement(input: SeparationCorpsPrefillInput): string | null {
  const v = input.aiData?.dateSeparation;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePatrimoineCommun(input: SeparationCorpsPrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommun;
  return typeof v === 'boolean' ? v : null;
}

/**
 * SF-246-08 : montant du patrimoine commun (€) — invariant §5.2 (> 0 ou null,
 * jamais 0). Distinct du boolean `patrimoineCommun` (flag régime).
 */
export function computePatrimoineCommunEur(input: SeparationCorpsPrefillInput): number | null {
  const v = input.aiData?.patrimoineCommunEur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: SeparationCorpsPrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
  let n = 0;
  if (computeDateJugement(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  if (computePatrimoineCommunEur(input) !== null) n++;
  return n;
}

export const SeparationCorpsPrefillRules = {
  ISO_DATE_REGEX,
  computeDateJugement,
  computePatrimoineCommun,
  computePatrimoineCommunEur,
  computePrefillCount,
};
