/**
 * F-236 SF-236-02 — Helper partagé pour la fiche Tribunal du travail (BE).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `TribunalTravailFicheSectionComponent.prefillFromAi()` :
 *   `procedureInfo.commissionParitaire ← aiData.conventionCollective`
 *   `contratInfo.typeContrat           ← normalize(aiData.typeContrat)` (EMPLOYE | OUVRIER)
 *   `contratInfo.dateDebut             ← aiData.dateEntree`
 *   `contratInfo.dateFin               ← aiData.dateLicenciement`
 *   `contratInfo.motifRupture          ← aiData.motifLicenciement`
 *   `requerant.nom                     ← aiData.nomSalarie`
 *   `requerant.prenom                  ← aiData.prenomSalarie`
 *   `requerant.domicile                ← aiData.adresseSalarie`
 *   `defendeur.nom                     ← aiData.nomEmployeur`
 *   `defendeur.siegeSocial             ← aiData.adresseEmployeur`
 *   `defendeur.numeroBce               ← aiData.bceEmployeur` (BE uniquement)
 *
 * SF-246-15 : ajout des 6 champs identités salarié/employeur déjà présents
 * dans `TravailExtractedData` côté backend, absents du DTO frontend avant
 * cette SF.
 */
import { TravailExtractedData } from '../../core/models/case-analysis.model';

function nonEmpty(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export interface TribunalTravailFichePrefillInput {
  aiData?: Partial<TravailExtractedData> | null;
}

export function computeCommissionParitaire(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.conventionCollective);
}

export function computeTypeContrat(
  input: TribunalTravailFichePrefillInput,
): 'EMPLOYE' | 'OUVRIER' | null {
  const raw = input.aiData?.typeContrat;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const u = raw.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  if (u.includes('EMPLOYE') || u === 'CDI' || u === 'CDD') return 'EMPLOYE';
  if (u.includes('OUVRIER')) return 'OUVRIER';
  return null;
}

export function computeDateDebut(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.dateEntree);
}

export function computeDateFin(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.dateLicenciement);
}

export function computeMotifRupture(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.motifLicenciement);
}

/** SF-246-15 */
export function computeNomRequerant(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.nomSalarie);
}

/** SF-246-15 */
export function computePrenomRequerant(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.prenomSalarie);
}

/** SF-246-15 */
export function computeDomicileRequerant(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.adresseSalarie);
}

/** SF-246-15 */
export function computeNomDefendeur(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.nomEmployeur);
}

/** SF-246-15 */
export function computeSiegeSocialDefendeur(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.adresseEmployeur);
}

/** SF-246-15 : numéro BCE de l'employeur (BE uniquement). */
export function computeNumeroBce(input: TribunalTravailFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.bceEmployeur);
}

export function computePrefillCount(input: TribunalTravailFichePrefillInput): number {
  let count = 0;
  if (computeCommissionParitaire(input) !== null) count++;
  if (computeTypeContrat(input) !== null) count++;
  if (computeDateDebut(input) !== null) count++;
  if (computeDateFin(input) !== null) count++;
  if (computeMotifRupture(input) !== null) count++;
  if (computeNomRequerant(input) !== null) count++;
  if (computePrenomRequerant(input) !== null) count++;
  if (computeDomicileRequerant(input) !== null) count++;
  if (computeNomDefendeur(input) !== null) count++;
  if (computeSiegeSocialDefendeur(input) !== null) count++;
  if (computeNumeroBce(input) !== null) count++;
  return count;
}

export const TribunalTravailFicheSectionPrefillRules = {
  computeCommissionParitaire,
  computeTypeContrat,
  computeDateDebut,
  computeDateFin,
  computeMotifRupture,
  computeNomRequerant,
  computePrenomRequerant,
  computeDomicileRequerant,
  computeNomDefendeur,
  computeSiegeSocialDefendeur,
  computeNumeroBce,
  computePrefillCount,
};
