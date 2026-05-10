/**
 * F-236 SF-236-02 — Helper partagé `AutoriteParentalePrefillRules`.
 * 5 champs : regimeExerciceActuel, dangerCaracterise, consentementAutreParent,
 * interferenceVieEnfant, ageEnfants (filtré).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { RegimeExercice } from '../../core/models/autorite-parentale.model';

export const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeExercice>([
  'CONJOINT',
  'EXCLUSIF_MERE',
  'EXCLUSIF_PERE',
  'DELEGATION_TIERS',
]);

type Ai = Partial<FamilleExtractedData> & {
  regimeExerciceActuel?: string | null;
  dangerCaracterise?: boolean | null;
  consentementAutreParent?: boolean | null;
  interferenceVieEnfant?: boolean | null;
  ageEnfants?: (number | null)[] | null;
};

export interface AutoriteParentalePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeRegimeActuel(input: AutoriteParentalePrefillInput): RegimeExercice | null {
  const raw = input.aiData?.regimeExerciceActuel;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const upper = raw.toUpperCase();
  return VALID_REGIMES.has(upper) ? (upper as RegimeExercice) : null;
}

export function computeDangerCaracterise(input: AutoriteParentalePrefillInput): boolean | null {
  const v = input.aiData?.dangerCaracterise;
  return typeof v === 'boolean' ? v : null;
}
export function computeConsentementAutreParent(input: AutoriteParentalePrefillInput): boolean | null {
  const v = input.aiData?.consentementAutreParent;
  return typeof v === 'boolean' ? v : null;
}
export function computeInterferenceVieEnfant(input: AutoriteParentalePrefillInput): boolean | null {
  const v = input.aiData?.interferenceVieEnfant;
  return typeof v === 'boolean' ? v : null;
}

export function computeAgeEnfants(input: AutoriteParentalePrefillInput): number[] {
  const ages = input.aiData?.ageEnfants;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 30,
  );
}

export function computePrefillCount(input: AutoriteParentalePrefillInput): number {
  let n = 0;
  if (computeRegimeActuel(input) !== null) n++;
  if (computeDangerCaracterise(input) !== null) n++;
  if (computeConsentementAutreParent(input) !== null) n++;
  if (computeInterferenceVieEnfant(input) !== null) n++;
  if (computeAgeEnfants(input).length > 0) n++;
  return n;
}

export const AutoriteParentalePrefillRules = {
  VALID_REGIMES,
  computeRegimeActuel,
  computeDangerCaracterise,
  computeConsentementAutreParent,
  computeInterferenceVieEnfant,
  computeAgeEnfants,
  computePrefillCount,
};
