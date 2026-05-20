/**
 * SF-206-04 — Helper partagé pour l'outil "Congés payés acquis pendant arrêt
 * maladie" (F-DT-75). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA (SF-206-03 backend) extrait un sous-objet
 * `conges_payes_arret_maladie_detail` (5 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'`
 * — régime L.3141-5 / L.3141-5-1 CT purement français, loi 22/04/2024).
 *
 *   typeArret               ← aiData.cpArretMaladieType (enum)
 *   nombreMoisArret         ← aiData.cpArretMaladieNombreMois (int > 0)
 *   salarieEncoreEnPoste    ← aiData.cpArretMaladieSalarieEnPoste (bool)
 *   dateRuptureContrat      ← aiData.cpArretMaladieDateRupture (ISO YYYY-MM-DD)
 *   joursCpDejaAccordes     ← aiData.cpArretMaladieJoursDejaAccordes (≥ 0)
 *   salaireBrutMensuel      ← aiData.salaireBrutMensuel (déjà extrait, > 0)
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CongesPayesArretMaladieTypeArret } from '../../core/models/conges-payes-arret-maladie.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const TYPE_ARRET_VALUES: readonly CongesPayesArretMaladieTypeArret[] = [
  'MALADIE_NON_PROFESSIONNELLE',
  'ACCIDENT_TRAVAIL_MALADIE_PRO',
];

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface CongesPayesArretMaladiePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'cpArretMaladieType'
    | 'cpArretMaladieNombreMois'
    | 'cpArretMaladieSalarieEnPoste'
    | 'cpArretMaladieDateRupture'
    | 'cpArretMaladieJoursDejaAccordes'
    | 'salaireBrutMensuel'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: CongesPayesArretMaladiePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Normalise une chaîne enum côté IA (toUpperCase + trim) — null si vide / non whitelisté. */
function normalizeEnum<T extends string>(
  raw: string | null | undefined,
  whitelist: readonly T[],
): T | null {
  if (!raw || typeof raw !== 'string') return null;
  const candidate = raw.trim().toUpperCase() as T;
  return whitelist.includes(candidate) ? candidate : null;
}

/** `typeArret` — enum MALADIE_NON_PROFESSIONNELLE | ACCIDENT_TRAVAIL_MALADIE_PRO. */
export function computeTypeArret(
  input: CongesPayesArretMaladiePrefillInput,
): CongesPayesArretMaladieTypeArret | null {
  if (!isFrance(input)) return null;
  return normalizeEnum<CongesPayesArretMaladieTypeArret>(
    input.aiData?.cpArretMaladieType,
    TYPE_ARRET_VALUES,
  );
}

/** `nombreMoisArret` — entier strictement positif. */
export function computeNombreMoisArret(input: CongesPayesArretMaladiePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cpArretMaladieNombreMois;
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return Math.trunc(v);
}

/** `salarieEncoreEnPoste` — booléen factuel direct. */
export function computeSalarieEnPoste(input: CongesPayesArretMaladiePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cpArretMaladieSalarieEnPoste;
  return typeof v === 'boolean' ? v : null;
}

/** `dateRuptureContrat` — date ISO YYYY-MM-DD. */
export function computeDateRupture(input: CongesPayesArretMaladiePrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cpArretMaladieDateRupture;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `joursCpDejaAccordes` — décimal ≥ 0 (0 accepté). */
export function computeJoursDejaAccordes(
  input: CongesPayesArretMaladiePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.cpArretMaladieJoursDejaAccordes;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return v;
}

/** `salaireBrutMensuel` — décimal > 0 (déjà extrait par le pré-fill existant). */
export function computeSalaireBrutMensuel(
  input: CongesPayesArretMaladiePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.salaireBrutMensuel;
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 */
export function computePrefillCount(
  input: CongesPayesArretMaladiePrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeTypeArret(input) !== null) count++;
  if (computeNombreMoisArret(input) !== null) count++;
  if (computeSalarieEnPoste(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  if (computeJoursDejaAccordes(input) !== null) count++;
  if (computeSalaireBrutMensuel(input) !== null) count++;
  return count;
}

export const CongesPayesArretMaladieSectionPrefillRules = {
  computeTypeArret,
  computeNombreMoisArret,
  computeSalarieEnPoste,
  computeDateRupture,
  computeJoursDejaAccordes,
  computeSalaireBrutMensuel,
  computePrefillCount,
};
