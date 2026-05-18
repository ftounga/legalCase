/**
 * F-236 SF-236-02 / SF-246-02 — Helper partagé pour l'outil "Clause de
 * non-concurrence" (F-DT-24, FR). Module pur — runtime (`prefillFromAi()`) et
 * static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236).
 *
 * SF-246-02 : le pipeline IA extrait désormais le sous-objet
 * `clause_non_concurrence_detail` (cf. `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`).
 * 8 champs du formulaire sont pré-remplissables, FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'`) :
 *
 *   salaireMensuelBrutEur         ← aiData.salaireBrutMensuel (déjà branché)
 *   clausePresenteContrat         ← aiData.clauseNonConcurrenceDetectee (flag F-166)
 *   dureeMois                     ← aiData.nonConcurrenceDureeMois
 *   limiteDureeDefinie            ← dérivé : true si dureeMois != null
 *   territoireDescription         ← aiData.nonConcurrenceZoneGeographique
 *   limiteTerritoireDefini        ← dérivé : true si zone non vide
 *   contrepartieMontantMensuelEur ← aiData.nonConcurrenceContrepartieMontantEur
 *   contrepartieFinancierePresente ← dérivé : true si contrepartie != null
 *
 * Les 4 booléens dérivés sont calculés ici à partir des champs source — pas de
 * champ booléen redondant dans le record IA (invariant « un champ = une
 * définition », cf. mini-spec §"Note de design IA").
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** SF-246-02 : contrat figé — sous-ensemble de `TravailExtractedData` consommé par le pré-fill. */
export interface NonConcurrencePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'salaireBrutMensuel'
    | 'clauseNonConcurrenceDetectee'
    | 'nonConcurrenceDureeMois'
    | 'nonConcurrenceZoneGeographique'
    | 'nonConcurrenceContrepartieMontantEur'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: NonConcurrencePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** `salaireMensuelBrutEur` — montant brut mensuel strictement positif. */
export function computeSalaireMensuelBrutEur(input: NonConcurrencePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * `clausePresenteContrat` — booléen de présence de la clause au contrat.
 * Mappé depuis le flag de visibilité `clauseNonConcurrenceDetectee` (F-166),
 * qui constate textuellement la présence de la clause aux pièces.
 */
export function computeClausePresenteContrat(input: NonConcurrencePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.clauseNonConcurrenceDetectee;
  return typeof v === 'boolean' ? v : null;
}

/** `dureeMois` — durée de la clause en mois, bornée `[0, 600]`. */
export function computeDureeMois(input: NonConcurrencePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.nonConcurrenceDureeMois;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  return v >= 0 && v <= 600 ? v : null;
}

/** `limiteDureeDefinie` — dérivé : true si une durée a été détectée. */
export function computeLimiteDureeDefinie(input: NonConcurrencePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return computeDureeMois(input) !== null ? true : null;
}

/** `territoireDescription` — zone géographique en texte libre non vide. */
export function computeTerritoireDescription(input: NonConcurrencePrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.nonConcurrenceZoneGeographique;
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  return trimmed.length > 0 ? trimmed : null;
}

/** `limiteTerritoireDefini` — dérivé : true si une zone non vide a été détectée. */
export function computeLimiteTerritoireDefini(input: NonConcurrencePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return computeTerritoireDescription(input) !== null ? true : null;
}

/** `contrepartieMontantMensuelEur` — montant brut mensuel strictement positif. */
export function computeContrepartieMontantEur(input: NonConcurrencePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.nonConcurrenceContrepartieMontantEur;
  return typeof v === 'number' && Number.isFinite(v) && v > 0 ? v : null;
}

/** `contrepartieFinancierePresente` — dérivé : true si une contrepartie a été détectée. */
export function computeContrepartieFinancierePresente(input: NonConcurrencePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return computeContrepartieMontantEur(input) !== null ? true : null;
}

/**
 * Nombre exact de champs effectivement pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Compte les 8 champs : 3 champs valeur (salaire, durée, contrepartie),
 * 1 booléen factuel (clause présente) et 4 booléens dérivés (limites + présence
 * contrepartie). Un booléen dérivé n'est compté que si son champ source l'est.
 */
export function computePrefillCount(input: NonConcurrencePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeClausePresenteContrat(input) !== null) count++;
  if (computeDureeMois(input) !== null) count++;
  if (computeLimiteDureeDefinie(input) !== null) count++;
  if (computeTerritoireDescription(input) !== null) count++;
  if (computeLimiteTerritoireDefini(input) !== null) count++;
  if (computeContrepartieMontantEur(input) !== null) count++;
  if (computeContrepartieFinancierePresente(input) !== null) count++;
  return count;
}

export const NonConcurrenceSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeClausePresenteContrat,
  computeDureeMois,
  computeLimiteDureeDefinie,
  computeTerritoireDescription,
  computeLimiteTerritoireDefini,
  computeContrepartieMontantEur,
  computeContrepartieFinancierePresente,
  computePrefillCount,
};
