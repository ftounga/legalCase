/**
 * SF-216-16 — Helper partagé pour l'outil "Adoption de l'enfant du conjoint
 * (adoption intra-familiale)" (F-FA-ADOPTION-INTRA). Module pur — runtime
 * (`prefillFromAi()`) et static (`getPrefillCount()`) appellent les MÊMES
 * fonctions (contrat F-236 / F-237).
 *
 * V1 — PREFILL_COUNT_ALWAYS_ZERO :
 *   La détection IA `adoption_intra_detection.envisagee` (SF-216-15) sert
 *   uniquement à activer la visibilité du panel F-IA-04 (mode CONTEXTUAL).
 *   Aucun champ saisissable du formulaire n'est pré-rempli en V1 : le
 *   simulateur exige une saisie active de l'avocat (forme demandée, lien
 *   matrimonial, situation de l'autre parent biologique, consentement de
 *   l'enfant — données non extractibles avec certitude par l'IA).
 *
 *   En cas d'évolution future (V2), ajouter ici les mappers correspondants
 *   ET incrémenter `computePrefillCount()` (contrat F-237 strict).
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 345-1 Cciv).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AdoptionIntraFrPrefillInput {
  aiData?: Pick<FamilleExtractedData, never> | null;
  workspaceCountry?: string;
}

/**
 * Nombre exact de champs pré-remplissables. V1 — toujours 0.
 *
 * Conserve la signature symétrique avec les autres outils F-216 pour respecter
 * le contrat F-237 (parité runtime/static). Le test
 * `prefill-count-integrity.spec.ts` consomme cette fonction telle quelle.
 */
export function computePrefillCount(
  _input: AdoptionIntraFrPrefillInput,
): number {
  return 0;
}

export const AdoptionIntraFrPrefillRules = {
  computePrefillCount,
};
