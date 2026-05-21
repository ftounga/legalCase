/**
 * SF-216-06 — Helper partagé pour l'outil "Liquidation communauté légale" (F-FA-04).
 * Module pur — runtime (`prefillFromAi()`) et static (`getPrefillCount()`)
 * appellent les MÊMES fonctions (contrat F-236 / F-237 / F-246).
 *
 * Le pipeline IA SF-216-05 backend extrait :
 *  - `regime_matrimonial_detection.regime_matrimonial`     (déjà branché F-246)
 *  - `regime_matrimonial_detection.valeur_communaute_eur`  (déjà branché F-246)
 *  - `communaute_partage_protection_detection_v2.clause_attribution_integrale_detected`
 *    (déjà branché F-246)
 *  - `liquidation_communaute_detection.envisagee`           (NOUVEAU SF-216-05, flag CONTEXTUAL)
 *  - `liquidation_communaute_detection.recompenses_epoux1_eur` (NOUVEAU SF-216-05)
 *  - `liquidation_communaute_detection.recompenses_epoux2_eur` (NOUVEAU SF-216-05)
 *
 * Le record Java `FamilleExtractedData` projette ces 5 champs à plat dans le
 * `familleExtractedData` du synthesis frontend. Noms Jackson :
 *   `regimeMatrimonialDetecte`, `valeurCommunauteEurDetectee`,
 *   `clauseAttributionIntegraleDetected`, `recompensesEpoux1Eur`,
 *   `recompensesEpoux2Eur`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. 1467 Cciv).
 *
 *   regimeMatrimonialDetecte (lecture seule, alerte si != COMMUNAUTE_LEGALE)
 *   recompensesEpoux1Eur     ← aiData.recompensesEpoux1Eur
 *   recompensesEpoux2Eur     ← aiData.recompensesEpoux2Eur
 *
 * `valeurCommunauteEurDetectee` et `clauseAttributionIntegraleDetected` sont
 * lus pour affichage informatif uniquement (pas de remplissage 1-pour-1 d'un
 * champ formulaire — la valeur communauté est éclatée en immeuble/mobilier
 * dans le formulaire, et la clause attribution intégrale déclenche une
 * bannière d'alerte renvoyant vers F-FA-16-communaute-universelle).
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface LiquidationCommunauteFrPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'regimeMatrimonialDetecte'
    | 'valeurCommunauteEurDetectee'
    | 'clauseAttributionIntegraleDetected'
    | 'recompensesEpoux1Eur'
    | 'recompensesEpoux2Eur'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: LiquidationCommunauteFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return Math.trunc(v);
}

/**
 * Régime matrimonial détecté (lecture seule pour affichage informatif).
 * Le composant utilise ce signal pour afficher une bannière si le régime
 * détecté est différent de COMMUNAUTE_LEGALE.
 */
export function computeRegimeMatrimonialDetecte(
  input: LiquidationCommunauteFrPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.regimeMatrimonialDetecte;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/**
 * Détection clause d'attribution intégrale (art. 1527 al. 1 Cciv).
 * Si true, déclenche une bannière renvoyant vers l'outil
 * F-FA-16-communaute-universelle (régime conventionnel distinct).
 */
export function computeClauseAttributionIntegrale(
  input: LiquidationCommunauteFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.clauseAttributionIntegraleDetected;
  return typeof v === 'boolean' ? v : null;
}

/** Récompenses dues par l'époux 1 à la communauté (€, ≥ 0). */
export function computeRecompensesEpoux1(
  input: LiquidationCommunauteFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.recompensesEpoux1Eur);
}

/** Récompenses dues par l'époux 2 à la communauté (€, ≥ 0). */
export function computeRecompensesEpoux2(
  input: LiquidationCommunauteFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.recompensesEpoux2Eur);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 *
 * Comptés ici : les champs IA qui alimentent une saisie du formulaire (récompenses 1/2).
 * `regimeMatrimonialDetecte` et `clauseAttributionIntegraleDetected` sont des signaux
 * d'affichage/alerte (pas de champ formulaire dédié) — ils ne comptent pas.
 */
export function computePrefillCount(
  input: LiquidationCommunauteFrPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeRecompensesEpoux1(input) !== null) count++;
  if (computeRecompensesEpoux2(input) !== null) count++;
  return count;
}

export const LiquidationCommunauteFrPrefillRules = {
  computeRegimeMatrimonialDetecte,
  computeClauseAttributionIntegrale,
  computeRecompensesEpoux1,
  computeRecompensesEpoux2,
  computePrefillCount,
};
