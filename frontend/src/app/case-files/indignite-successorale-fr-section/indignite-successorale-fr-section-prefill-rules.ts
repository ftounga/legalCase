/**
 * SF-216-20 — Helper partagé pour l'outil "Indignité successorale"
 * (F-FA-INDIGNITE-SUCCESSORALE). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 3 champs pré-remplissables :
 *   - `condamnationPrononcee`        ← `condamnationPenaleSuccessionDetectee`
 *   - `pardonTestamentaireDetecte`   ← `pardonTestamentaireDetecte`
 *   - `dateOuvertureSuccession`      ← `dateOuvertureSuccessionDetectee`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 726 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface IndigniteSuccessoraleFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'condamnationPenaleSuccessionDetectee'
        | 'pardonTestamentaireDetecte'
        | 'dateOuvertureSuccessionDetectee'
      >
    | null;
  workspaceCountry?: string;
}

export interface IndigniteSuccessoralePrefilledValues {
  condamnationPrononcee: boolean | null;
  pardonTestamentaireDetecte: boolean | null;
  dateOuvertureSuccession: string | null;
}

/**
 * Retourne le triplet (condamnation, pardon, date ouverture) extrait de
 * l'IA si disponible. Hors France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: IndigniteSuccessoraleFrPrefillInput,
): IndigniteSuccessoralePrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return {
      condamnationPrononcee: null,
      pardonTestamentaireDetecte: null,
      dateOuvertureSuccession: null,
    };
  }
  const ai = input.aiData;
  if (!ai) {
    return {
      condamnationPrononcee: null,
      pardonTestamentaireDetecte: null,
      dateOuvertureSuccession: null,
    };
  }
  const condamnation =
    typeof ai.condamnationPenaleSuccessionDetectee === 'boolean'
      ? ai.condamnationPenaleSuccessionDetectee
      : null;
  const pardon =
    typeof ai.pardonTestamentaireDetecte === 'boolean'
      ? ai.pardonTestamentaireDetecte
      : null;
  const dateOuverture =
    typeof ai.dateOuvertureSuccessionDetectee === 'string' &&
    ai.dateOuvertureSuccessionDetectee.trim() !== ''
      ? ai.dateOuvertureSuccessionDetectee
      : null;
  return {
    condamnationPrononcee: condamnation,
    pardonTestamentaireDetecte: pardon,
    dateOuvertureSuccession: dateOuverture,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: IndigniteSuccessoraleFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.condamnationPrononcee !== null) count += 1;
  if (v.pardonTestamentaireDetecte !== null) count += 1;
  if (v.dateOuvertureSuccession !== null) count += 1;
  return count;
}

export const IndigniteSuccessoraleFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
