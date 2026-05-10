/**
 * F-236 SF-236-02 — Helper partagé `<ComponentName>PrefillRules`.
 *
 * <p>Module pur (aucun import Angular, aucun effet de bord) consommé par :
 * <ul>
 *   <li>`prefillFromAi()` runtime du composant (qui applique les valeurs aux signals)</li>
 *   <li>`static getPrefillCount(input)` du composant (qui compte sans instancier)</li>
 * </ul>
 *
 * La divergence runtime / static devient impossible par construction : les
 * deux chemins consomment exactement les mêmes fonctions pures sur les
 * mêmes inputs.
 *
 * Pattern : SF-177-12 (immigration-title-decision-section) étendu par F-236.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { RegimeMatrimonialRecompenses } from '../../core/models/recompenses.model';

/**
 * Régimes valides pour l'outil récompenses (cf. SF-FA-15-01 calculator).
 * `SEPARATION_BIENS` est exclu — l'outil ne s'applique pas (art. 1536 Cciv).
 */
export const VALID_REGIMES: ReadonlySet<RegimeMatrimonialRecompenses> = new Set<RegimeMatrimonialRecompenses>([
  'COMMUNAUTE_LEGALE',
  'PARTICIPATION_AUX_ACQUETS',
  'COMMUNAUTE_UNIVERSELLE',
]);

export interface RecompensesPrefillInput {
  aiData?: FamilleExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/**
 * Calcule le régime matrimonial pré-rempli depuis `aiData.regimeMatrimonialDetecte`.
 * Retourne `null` si :
 *  - `aiData` absent
 *  - champ détecté absent / non-string
 *  - régime hors `VALID_REGIMES` (SEPARATION_BIENS, inconnu)
 *
 * Stricte parité avec le runtime `prefillFromAi()` du composant.
 */
export function computeRegimeMatrimonial(
  input: RecompensesPrefillInput,
): RegimeMatrimonialRecompenses | null {
  const ai = input.aiData;
  if (!ai) return null;
  const detected = ai.regimeMatrimonialDetecte;
  if (typeof detected !== 'string' || !detected) return null;
  const normalized = detected.toUpperCase() as RegimeMatrimonialRecompenses;
  if (!VALID_REGIMES.has(normalized)) return null;
  return normalized;
}

/**
 * Compte le nombre de champs que `prefillFromAi()` runtime poserait depuis
 * `aiData` (et autres sources). Utilisé par le panel F-IA-04 pour le badge
 * `auto_awesome (+N)` AVANT instanciation du composant.
 */
export function computePrefillCount(input: RecompensesPrefillInput): number {
  let count = 0;
  if (computeRegimeMatrimonial(input) !== null) count++;
  return count;
}

/**
 * Surface unifiée — alias d'objet pour rester aligné avec la convention
 * `<ComponentName>PrefillRules.computePrefillCount(input)`.
 */
export const RecompensesPrefillRules = {
  VALID_REGIMES,
  computeRegimeMatrimonial,
  computePrefillCount,
};
