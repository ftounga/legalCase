/**
 * F-236 SF-236-02 — Helper partagé `DesaccordsParentauxPrefillRules`.
 * 5 champs : domaine, intensite, tentativesMediation (array filtré),
 * agesEnfantsConcernes (filtré), urgence (bool).
 *
 * SF-246-10 : `ageEnfants` aspirationnel remplacé par `agesEnfantsDetectes`
 * (source backend réelle : `autorite_parentale_detection.ages_enfants`).
 * Plage [0, 25] (alignée sur le contrat backend).
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

type Ai = Partial<FamilleExtractedData>;

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

/** SF-246-10 : âges des enfants depuis le champ réel `agesEnfantsDetectes`. */
export function computeAgesEnfants(input: DesaccordsParentauxPrefillInput): number[] {
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 25,
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
  if (computeAgesEnfants(input).length > 0) n++;
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
  computeAgesEnfants,
  computeUrgence,
  computePrefillCount,
};
