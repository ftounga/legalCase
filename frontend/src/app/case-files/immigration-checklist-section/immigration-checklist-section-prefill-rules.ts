import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ImmigrationChecklistSectionComponent` (F-IM-01-checklist-pieces).
 *
 * Particularité : le composant n'a pas de méthode `prefillFromAi()`
 * runtime ; le pré-fill se fait via le setter `@Input set
 * inferredChecklistType()`. Le helper expose néanmoins
 * `computeInferredChecklistType` afin de garantir une parité strictement
 * structurelle avec le static `getPrefillCount` (mêmes regex / mêmes
 * sets / mêmes constantes).
 */

/**
 * F-177 SF-177-12 — Liste exhaustive des 13 régimes juridiques exposés par
 * `titreTypes` côté composant. Doit rester synchronisé avec la liste
 * `titreTypes` du composant (divergence rend le badge faux).
 */
export const KNOWN_TITRE_TYPES = new Set<string>([
  'VISA_ETUDIANT', 'APS_POST_ETUDES', 'TITRE_SALARIE', 'PASSEPORT_TALENT',
  'CST_VPF_CONJOINT_FR', 'CST_VPF_PARENT_ENFANT_FR', 'CST_VPF_LIENS_PERSONNELS',
  'REGROUPEMENT_FAMILIAL', 'ADMISSION_EXCEPTIONNELLE_AES', 'ASILE_OFPRA',
  'PROTECTION_SUBSIDIAIRE', 'CARTE_RESIDENT_10ANS', 'NATURALISATION',
]);

export const ImmigrationChecklistPrefillRules = {
  KNOWN_TITRE_TYPES,

  /**
   * Retourne la valeur `inferredChecklistType` si elle est exposée par l'IA
   * ET correspond à un des 13 régimes connus, sinon null.
   */
  computeInferredChecklistType(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const inferred = ai.inferredChecklistType;
    if (typeof inferred !== 'string' || inferred.length === 0) return null;
    return KNOWN_TITRE_TYPES.has(inferred) ? inferred : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    return this.computeInferredChecklistType(input) !== null ? 1 : 0;
  },
} as const;
