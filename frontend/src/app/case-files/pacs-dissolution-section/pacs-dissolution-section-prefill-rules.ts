/**
 * F-236 SF-236-02 — Helper partagé `PacsDissolutionPrefillRules`.
 * 5 champs : dateConclusionPacs (ISO), modeDissolution, regimeBiens,
 * creancesAlleguees (array filtré), patrimoineCommunSignificatif (bool).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  CreanceAlleguee,
  ModeDissolutionPacs,
  RegimeBiensPacs,
} from '../../core/models/pacs-dissolution.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_MODES: ReadonlySet<string> = new Set<ModeDissolutionPacs>([
  'DECLARATION_UNILATERALE', 'DECLARATION_CONJOINTE', 'MARIAGE_PARTENAIRES',
  'MARIAGE_TIERS', 'DECES',
]);
export const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeBiensPacs>([
  'SEPARATION_BIENS', 'INDIVISION_AMENAGEE', 'INDIVISION_PAR_DEFAUT',
]);
export const VALID_CREANCES: ReadonlySet<string> = new Set<CreanceAlleguee>([
  'CONTRIBUTION_DESEQUILIBRE', 'INVESTISSEMENT_BIEN_PROPRE',
  'ENRICHISSEMENT_INJUSTE', 'PRESTATION_TRAVAIL_NON_REMUNEREE', 'AUCUNE',
]);

type Ai = Partial<FamilleExtractedData> & {
  dateConclusionPacs?: string | null;
  modeDissolutionPacsDetecte?: string | null;
  regimeBiensPacsDetecte?: string | null;
  creancesAllegueesDetectees?: (string | null)[] | null;
  patrimoineCommunSignificatifDetecte?: boolean | null;
};

export interface PacsDissolutionPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateConclusion(input: PacsDissolutionPrefillInput): string | null {
  const v = input.aiData?.dateConclusionPacs;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computeModeDissolution(input: PacsDissolutionPrefillInput): ModeDissolutionPacs | null {
  const raw = input.aiData?.modeDissolutionPacsDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const upper = raw.toUpperCase();
  return VALID_MODES.has(upper) ? (upper as ModeDissolutionPacs) : null;
}

export function computeRegimeBiens(input: PacsDissolutionPrefillInput): RegimeBiensPacs | null {
  const raw = input.aiData?.regimeBiensPacsDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const upper = raw.toUpperCase();
  return VALID_REGIMES.has(upper) ? (upper as RegimeBiensPacs) : null;
}

export function computeCreances(input: PacsDissolutionPrefillInput): CreanceAlleguee[] {
  const raw = input.aiData?.creancesAllegueesDetectees;
  if (!Array.isArray(raw)) return [];
  return raw
    .filter((t): t is string => typeof t === 'string' && t.length > 0)
    .map(t => t.toUpperCase())
    .filter(t => VALID_CREANCES.has(t)) as CreanceAlleguee[];
}

export function computePatrimoineCommun(input: PacsDissolutionPrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommunSignificatifDetecte;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: PacsDissolutionPrefillInput): number {
  let n = 0;
  if (computeDateConclusion(input) !== null) n++;
  if (computeModeDissolution(input) !== null) n++;
  if (computeRegimeBiens(input) !== null) n++;
  if (computeCreances(input).length > 0) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  return n;
}

export const PacsDissolutionPrefillRules = {
  ISO_DATE_REGEX,
  VALID_MODES,
  VALID_REGIMES,
  VALID_CREANCES,
  computeDateConclusion,
  computeModeDissolution,
  computeRegimeBiens,
  computeCreances,
  computePatrimoineCommun,
  computePrefillCount,
};
