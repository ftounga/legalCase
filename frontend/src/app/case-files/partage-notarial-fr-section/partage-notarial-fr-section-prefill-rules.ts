/**
 * SF-216-28 — Helper partagé pour l'outil "Partage successoral notarié"
 * (F-FA-PARTAGE-NOTARIAL). Module pur — runtime (`prefillFromAi()`) et
 * static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 4 champs pré-remplissables :
 *   - `dateOuvertureSuccession`   ← `dateOuvertureSuccessionDetectee`
 *   - `nombreCoheritiers`         ← `nombreCoheritiersDetecte`
 *   - `valeurMasseSuccessoraleEur` ← `montantSuccessionEurDetecte`
 *   - `presenceImmeuble`          ← `presenceImmeubleSuccessionDetecte`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 816 et s. Cciv).
 * Aucun pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface PartageNotarialFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'dateOuvertureSuccessionDetectee'
        | 'nombreCoheritiersDetecte'
        | 'montantSuccessionEurDetecte'
        | 'presenceImmeubleSuccessionDetecte'
      >
    | null;
  workspaceCountry?: string;
}

export interface PartageNotarialPrefilledValues {
  /** ISO date YYYY-MM-DD ou null. */
  dateOuvertureSuccession: string | null;
  nombreCoheritiers: number | null;
  valeurMasseSuccessoraleEur: number | null;
  presenceImmeuble: boolean | null;
}

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const v = value.trim();
  return ISO_DATE_RE.test(v) ? v : null;
}

function positiveIntOrNull(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n) || n < 0 || !Number.isInteger(n)) return null;
  return n;
}

function nonNegativeNumberOrNull(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n) || n < 0) return null;
  // arrondi entier pour cohérence formulaire EUR (pas de centimes)
  return Math.round(n);
}

const EMPTY: PartageNotarialPrefilledValues = {
  dateOuvertureSuccession: null,
  nombreCoheritiers: null,
  valeurMasseSuccessoraleEur: null,
  presenceImmeuble: null,
};

/**
 * Retourne les valeurs pré-remplies depuis l'IA si disponibles. Hors
 * France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: PartageNotarialFrPrefillInput,
): PartageNotarialPrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') return { ...EMPTY };
  const ai = input.aiData;
  if (!ai) return { ...EMPTY };
  return {
    dateOuvertureSuccession: isoDateOrNull(ai.dateOuvertureSuccessionDetectee),
    nombreCoheritiers: positiveIntOrNull(ai.nombreCoheritiersDetecte),
    valeurMasseSuccessoraleEur: nonNegativeNumberOrNull(
      ai.montantSuccessionEurDetecte,
    ),
    presenceImmeuble:
      typeof ai.presenceImmeubleSuccessionDetecte === 'boolean'
        ? ai.presenceImmeubleSuccessionDetecte
        : null,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: PartageNotarialFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.dateOuvertureSuccession !== null) count += 1;
  if (v.nombreCoheritiers !== null) count += 1;
  if (v.valeurMasseSuccessoraleEur !== null) count += 1;
  if (v.presenceImmeuble !== null) count += 1;
  return count;
}

export const PartageNotarialFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
