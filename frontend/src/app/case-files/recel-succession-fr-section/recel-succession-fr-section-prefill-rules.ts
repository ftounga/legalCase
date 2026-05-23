/**
 * SF-216-22 — Helper partagé pour l'outil "Recel de succession"
 * (F-FA-RECEL-SUCCESSION). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 3 champs pré-remplissables :
 *   - `typeRecel`                  ← `typeRecelDetecte`
 *   - `preuveRecel`                ← `preuveRecelDetectee`
 *   - `dateOuvertureSuccession`    ← `dateOuvertureSuccessionDetectee` (F-246)
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 778 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { TypeRecel, PreuveRecel } from '../../core/models/recel-succession-fr.model';

/** Valeurs admises pour `typeRecel` (whitelist enum backend). */
const TYPES_RECEL_VALIDES: ReadonlySet<TypeRecel> = new Set<TypeRecel>([
  'DISSIMULATION_BIEN',
  'DISSIMULATION_DONATION',
  'DESTRUCTION_TESTAMENT',
  'RECEL_CREANCE',
  'AUTRE',
]);

/** Valeurs admises pour `preuveRecel` (whitelist enum backend). */
const PREUVES_RECEL_VALIDES: ReadonlySet<PreuveRecel> = new Set<PreuveRecel>([
  'AVEUX',
  'DOCUMENT',
  'TEMOIGNAGE',
  'EXPERTISE',
  'FAISCEAU_INDICES',
  'AUCUNE',
]);

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface RecelSuccessionFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'typeRecelDetecte'
        | 'preuveRecelDetectee'
        | 'dateOuvertureSuccessionDetectee'
      >
    | null;
  workspaceCountry?: string;
}

export interface RecelSuccessionPrefilledValues {
  typeRecel: TypeRecel | null;
  preuveRecel: PreuveRecel | null;
  dateOuvertureSuccession: string | null;
}

/**
 * Retourne le triplet (type, preuve, date ouverture) extrait de l'IA si
 * disponible. Hors France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: RecelSuccessionFrPrefillInput,
): RecelSuccessionPrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return {
      typeRecel: null,
      preuveRecel: null,
      dateOuvertureSuccession: null,
    };
  }
  const ai = input.aiData;
  if (!ai) {
    return {
      typeRecel: null,
      preuveRecel: null,
      dateOuvertureSuccession: null,
    };
  }
  const type =
    typeof ai.typeRecelDetecte === 'string' &&
    TYPES_RECEL_VALIDES.has(ai.typeRecelDetecte as TypeRecel)
      ? (ai.typeRecelDetecte as TypeRecel)
      : null;
  const preuve =
    typeof ai.preuveRecelDetectee === 'string' &&
    PREUVES_RECEL_VALIDES.has(ai.preuveRecelDetectee as PreuveRecel)
      ? (ai.preuveRecelDetectee as PreuveRecel)
      : null;
  const dateOuverture =
    typeof ai.dateOuvertureSuccessionDetectee === 'string' &&
    ai.dateOuvertureSuccessionDetectee.trim() !== ''
      ? ai.dateOuvertureSuccessionDetectee
      : null;
  return {
    typeRecel: type,
    preuveRecel: preuve,
    dateOuvertureSuccession: dateOuverture,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: RecelSuccessionFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.typeRecel !== null) count += 1;
  if (v.preuveRecel !== null) count += 1;
  if (v.dateOuvertureSuccession !== null) count += 1;
  return count;
}

export const RecelSuccessionFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
