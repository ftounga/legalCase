/**
 * SF-216-14 — Helper partagé pour l'outil "Audition du mineur par le JAF"
 * (F-FA-AUDITION-MINEUR). Module pur — runtime (`prefillFromAi()`) et
 * static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237).
 *
 * V1 — 2 champs pré-remplissables :
 *   - `ageEnfant`         ← `agesEnfantsDetectes[0]` (réutilisé F-246)
 *   - `demandeFormalisee` ← `demandeAuditionFormaliseeDetectee`
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 388-1 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AuditionMineurFrPrefillInput {
  aiData?:
    | Pick<FamilleExtractedData, 'agesEnfantsDetectes' | 'demandeAuditionFormaliseeDetectee'>
    | null;
  workspaceCountry?: string;
}

export interface AuditionMineurPrefilledValues {
  ageEnfant: number | null;
  demandeFormalisee: boolean | null;
}

/**
 * Retourne le couple (âge enfant, demande formalisée) extrait de l'IA si
 * disponible. Hors France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: AuditionMineurFrPrefillInput,
): AuditionMineurPrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return { ageEnfant: null, demandeFormalisee: null };
  }
  const ai = input.aiData;
  if (!ai) {
    return { ageEnfant: null, demandeFormalisee: null };
  }
  // ageEnfant ← premier âge détecté (si > 0, le calculateur acceptera 0
  // mais l'âge 0 n'a pas de sens métier — on pré-fill quand même).
  let ageEnfant: number | null = null;
  if (Array.isArray(ai.agesEnfantsDetectes) && ai.agesEnfantsDetectes.length > 0) {
    const first = ai.agesEnfantsDetectes[0];
    if (typeof first === 'number' && Number.isFinite(first) && first >= 0 && first < 18) {
      ageEnfant = first;
    }
  }
  const demandeFormalisee =
    typeof ai.demandeAuditionFormaliseeDetectee === 'boolean'
      ? ai.demandeAuditionFormaliseeDetectee
      : null;
  return { ageEnfant, demandeFormalisee };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: AuditionMineurFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.ageEnfant !== null) count += 1;
  if (v.demandeFormalisee !== null) count += 1;
  return count;
}

export const AuditionMineurFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
