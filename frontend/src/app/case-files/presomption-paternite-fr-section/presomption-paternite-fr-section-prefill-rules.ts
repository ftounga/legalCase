/**
 * SF-216-26 — Helper partagé pour l'outil "Présomption de paternité du
 * mari et désaveu" (F-FA-PRESOMPTION-PATERNITE). Module pur — runtime
 * (`prefillFromAi()`) et static (`getPrefillCount()`) appellent les MÊMES
 * fonctions (contrat F-236 / F-237).
 *
 * V1 — 5 champs pré-remplissables :
 *   - `dateNaissanceEnfant`           ← `dateNaissanceEnfantDetectee` (filiation_v2)
 *   - `possessionEtatConformeDetecte` ← `possessionEtatConforme5AnsDetected` (filiation_v2)
 *   - `dateConclusionMariage`         ← `dateConclusionMariageDetectee` (SF-216-25)
 *   - `dateDissolutionMariage`        ← `dateDissolutionMariageDetectee` (SF-216-25)
 *   - `desaveuEnvisage`               ← `desaveuEnvisage` (SF-216-25)
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 312 Cciv). Aucun
 * pré-fill hors FRANCE.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface PresomptionPaterniteFrPrefillInput {
  aiData?:
    | Pick<
        FamilleExtractedData,
        | 'dateNaissanceEnfantDetectee'
        | 'possessionEtatConforme5AnsDetected'
        | 'dateConclusionMariageDetectee'
        | 'dateDissolutionMariageDetectee'
        | 'desaveuEnvisage'
      >
    | null;
  workspaceCountry?: string;
}

export interface PresomptionPaternitePrefilledValues {
  dateNaissanceEnfant: string | null;
  possessionEtatConformeDetecte: boolean | null;
  dateConclusionMariage: string | null;
  dateDissolutionMariage: string | null;
  desaveuEnvisage: boolean | null;
}

/** Validation simple ISO date YYYY-MM-DD. */
function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const v = value.trim();
  if (v === '') return null;
  // Format YYYY-MM-DD strict.
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  return v;
}

/**
 * Retourne les valeurs pré-remplies depuis l'IA si disponibles. Hors
 * France : tout est null (gate single-country).
 */
export function prefillFromAi(
  input: PresomptionPaterniteFrPrefillInput,
): PresomptionPaternitePrefilledValues {
  if (input.workspaceCountry !== 'FRANCE') {
    return {
      dateNaissanceEnfant: null,
      possessionEtatConformeDetecte: null,
      dateConclusionMariage: null,
      dateDissolutionMariage: null,
      desaveuEnvisage: null,
    };
  }
  const ai = input.aiData;
  if (!ai) {
    return {
      dateNaissanceEnfant: null,
      possessionEtatConformeDetecte: null,
      dateConclusionMariage: null,
      dateDissolutionMariage: null,
      desaveuEnvisage: null,
    };
  }
  return {
    dateNaissanceEnfant: isoDateOrNull(ai.dateNaissanceEnfantDetectee),
    possessionEtatConformeDetecte:
      typeof ai.possessionEtatConforme5AnsDetected === 'boolean'
        ? ai.possessionEtatConforme5AnsDetected
        : null,
    dateConclusionMariage: isoDateOrNull(ai.dateConclusionMariageDetectee),
    dateDissolutionMariage: isoDateOrNull(ai.dateDissolutionMariageDetectee),
    desaveuEnvisage:
      typeof ai.desaveuEnvisage === 'boolean' ? ai.desaveuEnvisage : null,
  };
}

/**
 * Nombre exact de champs effectivement pré-remplis pour les inputs donnés.
 * Strictement aligné sur `prefillFromAi()` (contrat F-237).
 */
export function computePrefillCount(
  input: PresomptionPaterniteFrPrefillInput,
): number {
  const v = prefillFromAi(input);
  let count = 0;
  if (v.dateNaissanceEnfant !== null) count += 1;
  if (v.possessionEtatConformeDetecte !== null) count += 1;
  if (v.dateConclusionMariage !== null) count += 1;
  if (v.dateDissolutionMariage !== null) count += 1;
  if (v.desaveuEnvisage !== null) count += 1;
  return count;
}

export const PresomptionPaterniteFrPrefillRules = {
  computePrefillCount,
  prefillFromAi,
};
