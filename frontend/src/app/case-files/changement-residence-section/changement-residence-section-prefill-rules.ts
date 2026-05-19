/**
 * F-236 SF-236-02 — Helper partagé `ChangementResidencePrefillRules`.
 * 5 champs : raisonChangement, consentementAutreParent (bool),
 * informePrealablement (bool), modeResidenceActuel, agesEnfantsDetectes (filtré).
 *
 * SF-246-10 : `ageEnfants` aspirationnel remplacé par `agesEnfantsDetectes`
 * (source backend réelle : `autorite_parentale_detection.ages_enfants`).
 * Plage [0, 25] (alignée sur le contrat backend).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  ModeResidenceCh,
  RaisonChangement,
} from '../../core/models/changement-residence.model';

export const VALID_RAISONS: ReadonlySet<string> = new Set<RaisonChangement>([
  'TRAVAIL', 'FAMILLE', 'LOGEMENT', 'RAPPROCHEMENT_FAMILIAL', 'AUTRE',
]);
export const VALID_MODES: ReadonlySet<string> = new Set<ModeResidenceCh>([
  'ALTERNEE', 'EXCLUSIVE_DEMANDEUR', 'EXCLUSIVE_DEFENDEUR',
]);

type Ai = Partial<FamilleExtractedData>;

export interface ChangementResidencePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeRaisonChangement(input: ChangementResidencePrefillInput): RaisonChangement | null {
  const raw = input.aiData?.raisonChangementDetectee;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const upper = raw.toUpperCase();
  return VALID_RAISONS.has(upper) ? (upper as RaisonChangement) : null;
}

export function computeConsentementAutreParent(input: ChangementResidencePrefillInput): boolean | null {
  const v = input.aiData?.consentementAutreParent;
  return typeof v === 'boolean' ? v : null;
}

export function computeInformePrealablement(input: ChangementResidencePrefillInput): boolean | null {
  const v = input.aiData?.informePrealablement;
  return typeof v === 'boolean' ? v : null;
}

export function computeModeResidenceActuel(input: ChangementResidencePrefillInput): ModeResidenceCh | null {
  const raw = input.aiData?.modeResidenceActuel;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const upper = raw.toUpperCase();
  return VALID_MODES.has(upper) ? (upper as ModeResidenceCh) : null;
}

/** SF-246-10 : âges des enfants depuis le champ réel `agesEnfantsDetectes`. */
export function computeAgesEnfants(input: ChangementResidencePrefillInput): number[] {
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 25,
  );
}

export function computePrefillCount(input: ChangementResidencePrefillInput): number {
  let n = 0;
  if (computeRaisonChangement(input) !== null) n++;
  if (computeConsentementAutreParent(input) !== null) n++;
  if (computeInformePrealablement(input) !== null) n++;
  if (computeModeResidenceActuel(input) !== null) n++;
  if (computeAgesEnfants(input).length > 0) n++;
  return n;
}

export const ChangementResidencePrefillRules = {
  VALID_RAISONS,
  VALID_MODES,
  computeRaisonChangement,
  computeConsentementAutreParent,
  computeInformePrealablement,
  computeModeResidenceActuel,
  computeAgesEnfants,
  computePrefillCount,
};
