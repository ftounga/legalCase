/**
 * SF-212-04 — Helper partagé pour l'outil "Forfait jours — validité et
 * rappel HS" (F-DT-50). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-03 backend) extrait un sous-objet `forfait_jours_detail`
 * (5 champs) projeté à plat sur `TravailExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — le régime forfait jours L. 3121-58+ CT
 * avec exigences post-Cass. soc. 29/06/2011 est strictement français).
 *
 *   accordCollectifExiste         ← aiData.forfaitJoursAccordCollectifExiste
 *   entretienAnnuelRealise        ← aiData.forfaitJoursEntretienAnnuelRealise
 *   documentControleMensuelExiste ← aiData.forfaitJoursDocumentControle
 *   categorieAutonomeConfirmee    ← aiData.forfaitJoursCategorieAutonome
 *   nbJoursForfait                ← aiData.forfaitJoursNbJours
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ForfaitJoursFrPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'forfaitJoursAccordCollectifExiste'
    | 'forfaitJoursEntretienAnnuelRealise'
    | 'forfaitJoursDocumentControle'
    | 'forfaitJoursCategorieAutonome'
    | 'forfaitJoursNbJours'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ForfaitJoursFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Booléen direct — `accordCollectifExiste` (peut rester null si IA non tranché). */
export function computeAccordCollectifExiste(
  input: ForfaitJoursFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.forfaitJoursAccordCollectifExiste;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `entretienAnnuelRealise`. */
export function computeEntretienAnnuelRealise(
  input: ForfaitJoursFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.forfaitJoursEntretienAnnuelRealise;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `documentControleMensuelExiste`. */
export function computeDocumentControleMensuelExiste(
  input: ForfaitJoursFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.forfaitJoursDocumentControle;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `categorieAutonomeConfirmee`. */
export function computeCategorieAutonomeConfirmee(
  input: ForfaitJoursFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.forfaitJoursCategorieAutonome;
  return typeof v === 'boolean' ? v : null;
}

/** Entier borné [0, 235] — `nbJoursForfait`. */
export function computeNbJoursForfait(
  input: ForfaitJoursFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.forfaitJoursNbJours;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0 || v > 235) return null;
  return Math.trunc(v);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 5 champs IA.
 */
export function computePrefillCount(input: ForfaitJoursFrPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAccordCollectifExiste(input) !== null) count++;
  if (computeEntretienAnnuelRealise(input) !== null) count++;
  if (computeDocumentControleMensuelExiste(input) !== null) count++;
  if (computeCategorieAutonomeConfirmee(input) !== null) count++;
  if (computeNbJoursForfait(input) !== null) count++;
  return count;
}

export const ForfaitJoursFrSectionPrefillRules = {
  computeAccordCollectifExiste,
  computeEntretienAnnuelRealise,
  computeDocumentControleMensuelExiste,
  computeCategorieAutonomeConfirmee,
  computeNbJoursForfait,
  computePrefillCount,
};
