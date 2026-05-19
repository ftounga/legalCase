/**
 * F-236 SF-236-02 — Helper partagé `PrudhomeFicheSectionPrefillRules`.
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `PrudhomeFicheSectionComponent.prefillFromAi()` :
 *   `demandeur.profession   ← aiData.poste`
 *   `demandeur.nom          ← aiData.nomSalarie`
 *   `demandeur.prenom       ← aiData.prenomSalarie`
 *   `demandeur.adresse      ← aiData.adresseSalarie`
 *   `defendeur.nom          ← aiData.nomEmployeur`
 *   `defendeur.adresse      ← aiData.adresseEmployeur`
 *   `defendeur.siret        ← aiData.siretEmployeur` (FR uniquement)
 *
 * SF-246-15 : ajout des 6 champs identités salarié/employeur déjà présents
 * dans `TravailExtractedData` côté backend, absents du DTO frontend avant
 * cette SF.
 */
import { TravailExtractedData } from '../../core/models/case-analysis.model';

function nonEmpty(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export interface PrudhomeFichePrefillInput {
  aiData?: Partial<TravailExtractedData> | null;
}

export function computeProfession(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.poste);
}

/** SF-246-15 */
export function computeNomSalarie(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.nomSalarie);
}

/** SF-246-15 */
export function computePrenomSalarie(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.prenomSalarie);
}

/** SF-246-15 */
export function computeAdresseSalarie(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.adresseSalarie);
}

/** SF-246-15 */
export function computeNomEmployeur(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.nomEmployeur);
}

/** SF-246-15 */
export function computeAdresseEmployeur(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.adresseEmployeur);
}

/** SF-246-15 : SIRET employeur (FR uniquement). */
export function computeSiretEmployeur(input: PrudhomeFichePrefillInput): string | null {
  return nonEmpty(input.aiData?.siretEmployeur);
}

export function computePrefillCount(input: PrudhomeFichePrefillInput): number {
  let count = 0;
  if (computeProfession(input) !== null) count++;
  if (computeNomSalarie(input) !== null) count++;
  if (computePrenomSalarie(input) !== null) count++;
  if (computeAdresseSalarie(input) !== null) count++;
  if (computeNomEmployeur(input) !== null) count++;
  if (computeAdresseEmployeur(input) !== null) count++;
  if (computeSiretEmployeur(input) !== null) count++;
  return count;
}

export const PrudhomeFicheSectionPrefillRules = {
  computeProfession,
  computeNomSalarie,
  computePrenomSalarie,
  computeAdresseSalarie,
  computeNomEmployeur,
  computeAdresseEmployeur,
  computeSiretEmployeur,
  computePrefillCount,
};
