/**
 * F-236 SF-236-02 — Helper partagé `DesaccordsParentauxPrefillRules`.
 * 5 champs : domaine, intensite, tentativesMediation (array filtré),
 * ageEnfantsConcernes (filtré), urgence (bool).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  DomaineDesaccord,
  IntensiteDesaccord,
  TentativeMediation,
} from '../../core/models/desaccords-parentaux.model';

export const VALID_DOMAINES: ReadonlySet<string> = new Set<DomaineDesaccord>([
  'SCOLARITE', 'SANTE', 'RELIGION', 'LOISIRS_SPORTS', 'CHOIX_EDUCATIFS', 'DEMENAGEMENT', 'AUTRE',
]);
export const VALID_INTENSITES: ReadonlySet<string> = new Set<IntensiteDesaccord>([
  'MAJEUR', 'MOYEN', 'MINEUR',
]);
export const VALID_TENTATIVES: ReadonlySet<string> = new Set<TentativeMediation>([
  'MEDIATION_FAMILIALE', 'MEDIATION_JUDICIAIRE', 'DISCUSSIONS_DIRECTES', 'THERAPIE_FAMILIALE', 'AUCUNE',
]);

type Ai = Partial<FamilleExtractedData> & {
  domaineDesaccordDetecte?: string | null;
  intensiteDesaccordDetecte?: string | null;
  tentativesMediationDetectees?: (string | null)[] | null;
  ageEnfants?: (number | null)[] | null;
  urgenceDetectee?: boolean | null;
};

export interface DesaccordsParentauxPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDomaine(input: DesaccordsParentauxPrefillInput): DomaineDesaccord | null {
  const raw = input.aiData?.domaineDesaccordDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const upper = raw.toUpperCase();
  return VALID_DOMAINES.has(upper) ? (upper as DomaineDesaccord) : null;
}

export function computeIntensite(input: DesaccordsParentauxPrefillInput): IntensiteDesaccord | null {
  const raw = input.aiData?.intensiteDesaccordDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const upper = raw.toUpperCase();
  return VALID_INTENSITES.has(upper) ? (upper as IntensiteDesaccord) : null;
}

export function computeTentatives(input: DesaccordsParentauxPrefillInput): TentativeMediation[] {
  const raw = input.aiData?.tentativesMediationDetectees;
  if (!Array.isArray(raw)) return [];
  return raw
    .filter((t): t is string => typeof t === 'string' && t.length > 0)
    .map(t => t.toUpperCase())
    .filter(t => VALID_TENTATIVES.has(t)) as TentativeMediation[];
}

export function computeAgeEnfants(input: DesaccordsParentauxPrefillInput): number[] {
  const ages = input.aiData?.ageEnfants;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 30,
  );
}

export function computeUrgence(input: DesaccordsParentauxPrefillInput): boolean | null {
  const v = input.aiData?.urgenceDetectee;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: DesaccordsParentauxPrefillInput): number {
  let n = 0;
  if (computeDomaine(input) !== null) n++;
  if (computeIntensite(input) !== null) n++;
  if (computeTentatives(input).length > 0) n++;
  if (computeAgeEnfants(input).length > 0) n++;
  if (computeUrgence(input) !== null) n++;
  return n;
}

export const DesaccordsParentauxPrefillRules = {
  VALID_DOMAINES,
  VALID_INTENSITES,
  VALID_TENTATIVES,
  computeDomaine,
  computeIntensite,
  computeTentatives,
  computeAgeEnfants,
  computeUrgence,
  computePrefillCount,
};
