/**
 * F-236 SF-236-02 — Helper partagé `IndivisionSuccessoralePrefillRules`.
 * 2 champs : typeIndivision, dateOuvertureSuccession.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { TypeIndivisionSuccessorale } from '../../core/models/indivision-successorale.model';

type Ai = Partial<FamilleExtractedData> & {
  typeIndivisionSuccessoraleDetecte?: string | null;
  dateOuvertureSuccessionDetectee?: string | null;
};

export interface IndivisionSuccessoralePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseTypeFromIa(value: unknown): TypeIndivisionSuccessorale | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (!v) return null;
  if (v === 'INDIVISION_LEGALE' || v === 'LEGALE' || v === 'LEGAL') return 'INDIVISION_LEGALE';
  if (v === 'INDIVISION_CONVENTIONNELLE' || v === 'CONVENTIONNELLE' || v === 'CONVENTION')
    return 'INDIVISION_CONVENTIONNELLE';
  if (v === 'MAINTIEN_FORCE' || v === 'MAINTIEN' || v === 'FORCE') return 'MAINTIEN_FORCE';
  return null;
}

export function computeTypeIndivision(input: IndivisionSuccessoralePrefillInput): TypeIndivisionSuccessorale | null {
  return parseTypeFromIa(input.aiData?.typeIndivisionSuccessoraleDetecte);
}

export function computeDateOuverture(input: IndivisionSuccessoralePrefillInput): string | null {
  const v = input.aiData?.dateOuvertureSuccessionDetectee;
  return typeof v === 'string' && v.trim() ? v : null;
}

export function computePrefillCount(input: IndivisionSuccessoralePrefillInput): number {
  let n = 0;
  if (computeTypeIndivision(input) !== null) n++;
  if (computeDateOuverture(input) !== null) n++;
  return n;
}

export const IndivisionSuccessoralePrefillRules = {
  parseTypeFromIa,
  computeTypeIndivision,
  computeDateOuverture,
  computePrefillCount,
};
