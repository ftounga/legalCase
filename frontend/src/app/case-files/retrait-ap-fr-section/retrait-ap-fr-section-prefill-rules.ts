/**
 * SF-216-12 — Helper partagé pour l'outil "Retrait d'autorité parentale"
 * (F-FA-RETRAIT-AP). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-216-11 backend extrait :
 *  - `retrait_ap_detection.envisage`                       (flag CONTEXTUAL, non remplissable)
 *  - `retrait_ap_detection.condamnation_penale_detectee`   (NOUVEAU SF-216-11)
 *  - `retrait_ap_detection.violences_lmvss_2022_detectees` (NOUVEAU SF-216-11)
 *  - `protection_divorce_detection_v2.dangerCaracteriseDetecte`     (F-246)
 *  - `protection_divorce_detection_v2.violencesAllegueesDetectees`  (F-246)
 *  - `filiation_detection_v2.agesEnfantsDetectes`                   (F-246)
 *
 * Le record Java `FamilleExtractedData` projette ces champs à plat dans le
 * `familleExtractedData` du synthesis frontend. Noms Jackson :
 *   `retraitApEnvisage`, `condamnationPenaleDetectee`,
 *   `violencesLmvss2022Detectees`, `agesEnfantsDetectes`,
 *   `dangerImmediatDetected`, `violencesAllegueesDetectees`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 378-381 Cciv).
 *
 *   ageEnfant                    ← aiData.agesEnfantsDetectes[0]
 *   condamnationPenaleDetectee   ← aiData.condamnationPenaleDetectee
 *   dangerCaracterise            ← aiData.dangerImmediatDetected
 *   violencesConjugalesDetectees ← aiData.violencesAllegueesDetectees
 *                                  || aiData.violencesLmvss2022Detectees
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface RetraitApFrPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'agesEnfantsDetectes'
    | 'condamnationPenaleDetectee'
    | 'violencesLmvss2022Detectees'
    | 'dangerImmediatDetected'
    | 'violencesAllegueesDetectees'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: RetraitApFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  const i = Math.trunc(v);
  return i > 18 ? null : i;
}

/**
 * Âge du premier enfant détecté dans `filiation_detection_v2.agesEnfantsDetectes`.
 * Borné [0, 18] (mineurs uniquement — art. 371-1 Cciv).
 */
export function computeAgeEnfant(
  input: RetraitApFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages) || ages.length === 0) return null;
  return nonNegativeIntOrNull(ages[0]);
}

/** Flag condamnation pénale détecté dans le dossier (booléen strict). */
export function computeCondamnationPenale(
  input: RetraitApFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.condamnationPenaleDetectee;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Flag danger caractérisé détecté dans le dossier — réutilise le flag
 * `dangerImmediatDetected` déjà branché par F-246
 * (protection_divorce_detection_v2).
 */
export function computeDangerCaracterise(
  input: RetraitApFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dangerImmediatDetected;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Flag violences conjugales détectées — combinaison du flag F-246
 * (violences alléguées sur conjoint, liste typée non vide) et du nouveau flag
 * SF-216-11 (violences en présence de l'enfant loi 2022). True si l'un OU
 * l'autre documente la situation.
 */
export function computeViolencesConjugales(
  input: RetraitApFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const ai = input.aiData;
  if (!ai) return null;
  const lmvss = ai.violencesLmvss2022Detectees;
  const allegueesArr = ai.violencesAllegueesDetectees;
  const allegueesPresent = Array.isArray(allegueesArr) && allegueesArr.length > 0;
  if (lmvss === true || allegueesPresent) return true;
  if (lmvss === false) return false;
  return null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 *
 * Comptés ici : ageEnfant, condamnationPenaleDetectee, dangerCaracterise,
 * violencesConjugalesDetectees (4 champs).
 */
export function computePrefillCount(
  input: RetraitApFrPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAgeEnfant(input) !== null) count++;
  if (computeCondamnationPenale(input) !== null) count++;
  if (computeDangerCaracterise(input) !== null) count++;
  if (computeViolencesConjugales(input) !== null) count++;
  return count;
}

export const RetraitApFrPrefillRules = {
  computeAgeEnfant,
  computeCondamnationPenale,
  computeDangerCaracterise,
  computeViolencesConjugales,
  computePrefillCount,
};
