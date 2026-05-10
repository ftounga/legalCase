/**
 * F-236 SF-236-02 — Helper partagé `TestamentValiditePrefillRules`.
 * 4 champs : formeTestament, dateRedaction, saineDEsprit (bool),
 * legsExcedeQuotiteDisponible (bool).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { FormeTestament } from '../../core/models/testament-validite.model';

type Ai = Partial<FamilleExtractedData> & {
  formeTestamentDetectee?: string | null;
  dateRedactionTestamentDetectee?: string | null;
  saineDEspritTestateurDetected?: boolean | null;
  legsExcedeQuotiteDisponibleDetected?: boolean | null;
};

export interface TestamentValiditePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseFormeFromIa(value: unknown): FormeTestament | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (!v) return null;
  if (v === 'TESTAMENT_OLOGRAPHE' || v === 'OLOGRAPHE') return 'TESTAMENT_OLOGRAPHE';
  if (v === 'TESTAMENT_AUTHENTIQUE' || v === 'AUTHENTIQUE') return 'TESTAMENT_AUTHENTIQUE';
  if (v === 'TESTAMENT_MYSTIQUE' || v === 'MYSTIQUE') return 'TESTAMENT_MYSTIQUE';
  if (v === 'TESTAMENT_INTERNATIONAL' || v === 'INTERNATIONAL') return 'TESTAMENT_INTERNATIONAL';
  return null;
}

export function computeFormeTestament(input: TestamentValiditePrefillInput): FormeTestament | null {
  return parseFormeFromIa(input.aiData?.formeTestamentDetectee);
}

export function computeDateRedaction(input: TestamentValiditePrefillInput): string | null {
  const v = input.aiData?.dateRedactionTestamentDetectee;
  return typeof v === 'string' && v.trim() ? v : null;
}

export function computeSaineDEsprit(input: TestamentValiditePrefillInput): boolean | null {
  const v = input.aiData?.saineDEspritTestateurDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computeLegsExcedeQuotite(input: TestamentValiditePrefillInput): boolean | null {
  const v = input.aiData?.legsExcedeQuotiteDisponibleDetected;
  return v === null || v === undefined ? null : Boolean(v);
}

export function computePrefillCount(input: TestamentValiditePrefillInput): number {
  let n = 0;
  if (computeFormeTestament(input) !== null) n++;
  if (computeDateRedaction(input) !== null) n++;
  if (computeSaineDEsprit(input) !== null) n++;
  if (computeLegsExcedeQuotite(input) !== null) n++;
  return n;
}

export const TestamentValiditePrefillRules = {
  parseFormeFromIa,
  computeFormeTestament,
  computeDateRedaction,
  computeSaineDEsprit,
  computeLegsExcedeQuotite,
  computePrefillCount,
};
