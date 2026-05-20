/**
 * SF-246-11 / F-236 SF-236-02 — Helper partagé `ChangementEtatCivilPrefillRules`.
 *
 * Champ réellement pré-rempli par l'IA (source backend branchée SF-246-11) :
 *   - dateNaissanceDemandeur (ISO YYYY-MM-DD) ← `changement_etat_civil_detection.date_naissance_demandeur`
 *
 * Champs aspirationnels (dans le DTO frontend, absents du record backend — no-op gracieux) :
 *   - typeChangement, motifInvoque, majeurDemandeur, consentementParental
 *   Ces champs restent déclarés pour compatibilité avec le composant existant.
 *   `computePrefillCount()` ne les compte pas (ils retournent toujours undefined).
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

export interface ChangementEtatCivilPrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/** Aspirationnel — no-op gracieux (typeChangementDetecte absent du record backend). */
export function computeTypeChangement(input: ChangementEtatCivilPrefillInput): TypeChangement | null {
  const raw = (input.aiData as Record<string, unknown> | null | undefined)?.['typeChangementDetecte'];
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_TYPES.has(u) ? (u as TypeChangement) : null;
}

/** Aspirationnel — no-op gracieux (motifChangementDetecte absent du record backend). */
export function computeMotif(input: ChangementEtatCivilPrefillInput): MotifInvoque | null {
  const raw = (input.aiData as Record<string, unknown> | null | undefined)?.['motifChangementDetecte'];
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_MOTIFS.has(u) ? (u as MotifInvoque) : null;
}

/**
 * SF-246-11 : date de naissance du demandeur (ISO YYYY-MM-DD, Famille FR uniquement).
 * Source backend réelle : `changement_etat_civil_detection.date_naissance_demandeur`.
 * Guard `workspaceCountry === 'FRANCE'` — null pour la BE.
 */
export function computeDateNaissance(input: ChangementEtatCivilPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateNaissanceDemandeurDetectee;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** Aspirationnel — no-op gracieux (majeurDemandeurDetected absent du record backend). */
export function computeMajeurDemandeur(input: ChangementEtatCivilPrefillInput): boolean | null {
  const v = (input.aiData as Record<string, unknown> | null | undefined)?.['majeurDemandeurDetected'];
  return typeof v === 'boolean' ? v : null;
}

/** Aspirationnel — no-op gracieux (consentementParentalDetected absent du record backend). */
export function computeConsentementParental(input: ChangementEtatCivilPrefillInput): boolean | null {
  const v = (input.aiData as Record<string, unknown> | null | undefined)?.['consentementParentalDetected'];
  return typeof v === 'boolean' ? v : null;
}

/**
 * SF-246-11 : compte uniquement les champs réellement pré-remplis par l'IA.
 * Seul `dateNaissanceDemandeur` a une source backend (SF-246-11).
 * Les autres champs (typeChangement, motif, majeur, consentement) sont aspirationnels
 * et ne sont pas comptés — ils retournent toujours undefined côté runtime.
 */
export function computePrefillCount(input: ChangementEtatCivilPrefillInput): number {
  return computeDateNaissance(input) !== null ? 1 : 0;
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
