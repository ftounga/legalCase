/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil AT/MP.
 *
 * Module pur : pas d'import Angular, pas d'effet de bord, pas d'accès DOM.
 * La logique runtime (composant) ET le static `getPrefillCount` consomment
 * les mêmes fonctions ci-dessous sur le même input — la divergence devient
 * impossible par construction (cf. F-236 §Stratégie anti-divergence).
 *
 * Logique miroir de `AtMpSectionComponent.prefillFromAi()` :
 *   `dateAccident ← aiData.dateLicenciement` (proxy gracieux, AT seul).
 *
 * Le runtime gate sur `dispositif === 'RECONNAISSANCE_AT'` (état UI signal).
 * Pour le static (badge avant ouverture), on retient le **maximum potentiel**
 * accessible si l'avocat sélectionne RECONNAISSANCE_AT — soit 1 champ
 * pré-remplissable si l'IA fournit `dateLicenciement`.
 */

export interface AtMpPrefillInput {
  aiData?: { dateLicenciement?: string | null } | null;
}

/**
 * Calcule la date d'accident qu'on poserait dans le formulaire si l'avocat
 * sélectionne RECONNAISSANCE_AT. Retourne `null` si aucune donnée IA.
 */
export function computeDateAccident(input: AtMpPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const date = ai.dateLicenciement;
  if (typeof date !== 'string' || date.length === 0) return null;
  return date;
}

/**
 * Compte le nombre de champs que `prefillFromAi()` poserait (potentiellement)
 * sans instancier le composant. Stricte parité runtime ↔ static.
 */
export function computePrefillCount(input: AtMpPrefillInput): number {
  let count = 0;
  if (computeDateAccident(input) !== null) count++;
  return count;
}

/** Objet barrel pour les imports en miroir du nommage `<ComponentName>PrefillRules`. */
export const AtMpSectionPrefillRules = {
  computeDateAccident,
  computePrefillCount,
};
