/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil AT/MP.
 * SF-246-21 : correction du mapping douteux dateAccident ← dateLicenciement
 * + branchement de 2 nouveaux champs depuis `sante_discrimination_detection`
 * (atDateAccident, atDateExposition).
 *
 * Module pur : pas d'import Angular, pas d'effet de bord, pas d'accès DOM.
 * La logique runtime (composant) ET le static `getPrefillCount` consomment
 * les mêmes fonctions ci-dessous sur le même input — la divergence devient
 * impossible par construction (cf. F-236 §Stratégie anti-divergence).
 *
 * Logique miroir de `AtMpSectionComponent.prefillFromAi()` :
 *   `dateAccident   ← aiData.atDateAccident` (SF-246-21 — champ dédié) ;
 *                     fallback : `aiData.dateLicenciement` si atDateAccident absent.
 *   `dateExposition ← aiData.atDateExposition` (SF-246-21 — MP uniquement).
 *
 * Le runtime gate sur `dispositif === 'RECONNAISSANCE_AT'` (état UI signal).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface AtMpPrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    // SF-246-21 — sante_discrimination_detection
    atDateAccident?: string | null;
    atDateExposition?: string | null;
  } | null;
}

/**
 * SF-246-21 : date de l'accident du travail — champ dédié `atDateAccident`.
 * Fallback vers `dateLicenciement` si atDateAccident absent (rétrocompat).
 * Retourne `null` si aucune donnée IA.
 */
export function computeDateAccident(input: AtMpPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  // Priorité au champ dédié SF-246-21
  const atDate = ai.atDateAccident;
  if (typeof atDate === 'string' && ISO_DATE_RE.test(atDate)) return atDate;
  // Fallback rétrocompat : dateLicenciement (proxy gracieux, AT seul)
  const dateLic = ai.dateLicenciement;
  if (typeof dateLic === 'string' && dateLic.length > 0) return dateLic;
  return null;
}

/**
 * SF-246-21 : date de première exposition au risque (maladie professionnelle — ISO).
 * Distincte de la date d'accident AT.
 */
export function computeDateExposition(input: AtMpPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const v = ai.atDateExposition;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/**
 * Compte le nombre de champs que `prefillFromAi()` poserait (potentiellement)
 * sans instancier le composant. Stricte parité runtime ↔ static.
 */
export function computePrefillCount(input: AtMpPrefillInput): number {
  let count = 0;
  if (computeDateAccident(input) !== null) count++;
  if (computeDateExposition(input) !== null) count++;
  return count;
}

/** Objet barrel pour les imports en miroir du nommage `<ComponentName>PrefillRules`. */
export const AtMpSectionPrefillRules = {
  computeDateAccident,
  computeDateExposition,
  computePrefillCount,
};
