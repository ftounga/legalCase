/**
 * SF-212-38 — Helper partagé pour l'outil "Conciliation CPH — BCO"
 * (F-DT-84). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 /
 * F-237 / F-246).
 *
 * <p>Le pipeline IA (SF-212-37 backend) extrait un sous-objet
 * `conciliation_cph_detail` (3 champs) projeté à plat sur
 * `TravailExtractedData` via Jackson `@JsonUnwrapped`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — procédure prud'homale strictement
 * française ; la BE dispose d'un régime distinct tribunal du travail +
 * chambre de conciliation).</p>
 *
 *   ancienneteMois              ← aiData.conciliationCphAncienneteMois (entier)
 *   salaireMensuelBrutEuros     ← aiData.conciliationCphSalaire (decimal)
 *   montantDemandesEuros        ← aiData.conciliationCphMontantDemandes (decimal)
 *
 * <p>F-212 19/19 — dernier outil livré du bundle F-212.</p>
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ConciliationCphBcaPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'conciliationCphAncienneteMois'
    | 'conciliationCphSalaire'
    | 'conciliationCphMontantDemandes'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ConciliationCphBcaPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Entier — ancienneté en mois (1-600). Hors plage → null. */
export function computeAncienneteMois(
  input: ConciliationCphBcaPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.conciliationCphAncienneteMois;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (!Number.isInteger(v)) return null;
  if (v < 1 || v > 600) return null;
  return v;
}

/** Décimal — salaire mensuel brut (€). 1 200 ≤ v ≤ 50 000. */
export function computeSalaire(
  input: ConciliationCphBcaPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.conciliationCphSalaire;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 1200 || v > 50000) return null;
  return v;
}

/** Décimal — montant total des demandes (€). 500 ≤ v ≤ 1 000 000. */
export function computeMontantDemandes(
  input: ConciliationCphBcaPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.conciliationCphMontantDemandes;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 500 || v > 1000000) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 3 champs IA.
 */
export function computePrefillCount(
  input: ConciliationCphBcaPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeSalaire(input) !== null) count++;
  if (computeMontantDemandes(input) !== null) count++;
  return count;
}

export const ConciliationCphBcaSectionPrefillRules = {
  computeAncienneteMois,
  computeSalaire,
  computeMontantDemandes,
  computePrefillCount,
};
