/**
 * SF-212-34 — Helper partagé pour l'outil "Temps partiel — requalification
 * en temps plein" (F-DT-49). Module pur — runtime (`prefillFromAi()`) et
 * static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237 / F-246).
 *
 * Le pipeline IA (SF-212-33 backend) extrait un sous-objet
 * `temps_partiel_requalification_detail` (4 champs) projeté à plat sur
 * `TravailExtractedData` via Jackson `@JsonUnwrapped`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — le régime BE Loi 03/07/1978 + CCT
 * n°35 est juridiquement distinct).
 *
 *   dureeContractuelleHeures                ← aiData.tempsPartielDureeContractuelle
 *   mentionsDureePresentes                  ← aiData.tempsPartielMentionsDuree
 *   mentionsRepartitionPresentes            ← aiData.tempsPartielMentionsRepartition
 *   heuresComplementairesReelleMoyenneHeures ← aiData.tempsPartielHCMoyenne
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface TempsPartielRequalificationPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'tempsPartielDureeContractuelle'
    | 'tempsPartielMentionsDuree'
    | 'tempsPartielMentionsRepartition'
    | 'tempsPartielHCMoyenne'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: TempsPartielRequalificationPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Nombre décimal strictement positif et < 35 (cohérence temps partiel). */
export function computeDureeContractuelle(
  input: TempsPartielRequalificationPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.tempsPartielDureeContractuelle;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v <= 0 || v >= 35) return null;
  return v;
}

/** Booléen direct — `mentionsDureePresentes`. */
export function computeMentionsDuree(
  input: TempsPartielRequalificationPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.tempsPartielMentionsDuree;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `mentionsRepartitionPresentes`. */
export function computeMentionsRepartition(
  input: TempsPartielRequalificationPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.tempsPartielMentionsRepartition;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Heures complémentaires moyennes par semaine — borné à [0, 20].
 * Hors plage → null.
 */
export function computeHCMoyenne(
  input: TempsPartielRequalificationPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.tempsPartielHCMoyenne;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > 20) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 4 champs IA.
 */
export function computePrefillCount(
  input: TempsPartielRequalificationPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDureeContractuelle(input) !== null) count++;
  if (computeMentionsDuree(input) !== null) count++;
  if (computeMentionsRepartition(input) !== null) count++;
  if (computeHCMoyenne(input) !== null) count++;
  return count;
}

export const TempsPartielRequalificationSectionPrefillRules = {
  computeDureeContractuelle,
  computeMentionsDuree,
  computeMentionsRepartition,
  computeHCMoyenne,
  computePrefillCount,
};
