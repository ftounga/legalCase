/**
 * SF-216-18 — Helper partagé pour l'outil "Adoption internationale"
 * (F-FA-ADOPTION-INTERNATIONALE). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 3 champs pré-remplissables :
 *   - `paysOrigineEnfant`       ← `paysOrigineAdopteDetecte`
 *   - `agrement2025`            ← `agrement2025DetecteValide`
 *   - `exequaturRequis`         ← `exequaturRequisDetecte`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 370-3 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AdoptionInternationaleFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        'paysOrigineAdopteDetecte' | 'agrement2025DetecteValide' | 'exequaturRequisDetecte'
      >
    | null;
  workspaceCountry?: string;
}

export interface AdoptionInternationalePrefilledValues {
  paysOrigineEnfant: string | null;
  agrement2025: boolean | null;
  exequaturRequis: boolean | null;
}

/**
 * Retourne le triplet (pays, agrément, exequatur) extrait de l'IA si
 * disponible. Hors France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: AdoptionInternationaleFrPrefillInput,
): AdoptionInternationalePrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return { paysOrigineEnfant: null, agrement2025: null, exequaturRequis: null };
  }
  const ai = input.aiData;
  if (!ai) {
    return { paysOrigineEnfant: null, agrement2025: null, exequaturRequis: null };
  }
  const pays =
    typeof ai.paysOrigineAdopteDetecte === 'string' && ai.paysOrigineAdopteDetecte.trim() !== ''
      ? ai.paysOrigineAdopteDetecte
      : null;
  const agrement =
    typeof ai.agrement2025DetecteValide === 'boolean' ? ai.agrement2025DetecteValide : null;
  const exequatur =
    typeof ai.exequaturRequisDetecte === 'boolean' ? ai.exequaturRequisDetecte : null;
  return {
    paysOrigineEnfant: pays,
    agrement2025: agrement,
    exequaturRequis: exequatur,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: AdoptionInternationaleFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.paysOrigineEnfant !== null) count += 1;
  if (v.agrement2025 !== null) count += 1;
  if (v.exequaturRequis !== null) count += 1;
  return count;
}

export const AdoptionInternationaleFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
