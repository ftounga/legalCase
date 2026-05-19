/**
 * F-236 SF-236-02 — Helper partagé `AutoriteParentalePrefillRules`.
 * 5 champs : regimeExerciceActuel, dangerCaracterise, consentementAutreParent,
 * interferenceVieEnfant, agesEnfantsDetectes (filtré).
 *
 * SF-246-10 : `ageEnfants` aspirationnel remplacé par `agesEnfantsDetectes`
 * (source backend réelle : `autorite_parentale_detection.ages_enfants`).
 * Plage [0, 25] (vs [0, 30] de l'aspirationnel — alignée sur le contrat backend).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { RegimeExercice } from '../../core/models/autorite-parentale.model';

export const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeExercice>([
  'CONJOINT',
  'EXCLUSIF_MERE',
  'EXCLUSIF_PERE',
  'DELEGATION_TIERS',
]);

type Ai = Partial<FamilleExtractedData>;

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

/** SF-246-10 : âges des enfants depuis le champ réel `agesEnfantsDetectes`. */
export function computeAgesEnfants(input: AutoriteParentalePrefillInput): number[] {
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 25,
  );
}

export function computePrefillCount(input: AutoriteParentalePrefillInput): number {
  let n = 0;
  if (computeRegimeActuel(input) !== null) n++;
  if (computeDangerCaracterise(input) !== null) n++;
  if (computeConsentementAutreParent(input) !== null) n++;
  if (computeInterferenceVieEnfant(input) !== null) n++;
  if (computeAgesEnfants(input).length > 0) n++;
  return n;
}

export const AutoriteParentalePrefillRules = {
  VALID_REGIMES,
  computeRegimeActuel,
  computeDangerCaracterise,
  computeConsentementAutreParent,
  computeInterferenceVieEnfant,
  computeAgesEnfants,
  computePrefillCount,
};
