/**
 * F-236 SF-236-02 — Helper partagé `ChangementEtatCivilPrefillRules`.
 * 5 champs : typeChangement, motifInvoque, dateNaissanceDemandeur (ISO),
 * majeurDemandeur (bool), consentementParental (bool).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  MotifInvoque,
  TypeChangement,
} from '../../core/models/changement-etat-civil.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_TYPES: ReadonlySet<string> = new Set<TypeChangement>([
  'NOM', 'PRENOM', 'SEXE', 'NOM_ET_PRENOM',
]);
export const VALID_MOTIFS: ReadonlySet<string> = new Set<MotifInvoque>([
  'INTERET_LEGITIME', 'MARIAGE', 'RECTIFICATION_ERREUR', 'IDENTIFICATION_GENRE', 'AUTRE',
]);

type Ai = Partial<FamilleExtractedData> & {
  typeChangementDetecte?: string | null;
  motifChangementDetecte?: string | null;
  dateNaissanceDemandeurDetectee?: string | null;
  majeurDemandeurDetected?: boolean | null;
  consentementParentalDetected?: boolean | null;
};

export interface ChangementEtatCivilPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeTypeChangement(input: ChangementEtatCivilPrefillInput): TypeChangement | null {
  const raw = input.aiData?.typeChangementDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_TYPES.has(u) ? (u as TypeChangement) : null;
}

export function computeMotif(input: ChangementEtatCivilPrefillInput): MotifInvoque | null {
  const raw = input.aiData?.motifChangementDetecte;
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_MOTIFS.has(u) ? (u as MotifInvoque) : null;
}

export function computeDateNaissance(input: ChangementEtatCivilPrefillInput): string | null {
  const v = input.aiData?.dateNaissanceDemandeurDetectee;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computeMajeurDemandeur(input: ChangementEtatCivilPrefillInput): boolean | null {
  const v = input.aiData?.majeurDemandeurDetected;
  return typeof v === 'boolean' ? v : null;
}

export function computeConsentementParental(input: ChangementEtatCivilPrefillInput): boolean | null {
  const v = input.aiData?.consentementParentalDetected;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: ChangementEtatCivilPrefillInput): number {
  let n = 0;
  if (computeTypeChangement(input) !== null) n++;
  if (computeMotif(input) !== null) n++;
  if (computeDateNaissance(input) !== null) n++;
  if (computeMajeurDemandeur(input) !== null) n++;
  if (computeConsentementParental(input) !== null) n++;
  return n;
}

export const ChangementEtatCivilPrefillRules = {
  ISO_DATE_REGEX,
  VALID_TYPES,
  VALID_MOTIFS,
  computeTypeChangement,
  computeMotif,
  computeDateNaissance,
  computeMajeurDemandeur,
  computeConsentementParental,
  computePrefillCount,
};
