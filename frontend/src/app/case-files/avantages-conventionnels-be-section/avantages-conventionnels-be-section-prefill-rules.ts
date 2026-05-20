/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Avantages conventionnels" (BE).
 * SF-246-23 — Ajout des 3 champs manquants : commissionParitaireBe,
 * joursTravaillesAnneePrecedenteBe, joursPrestesBe (sources backend réelles
 * depuis travail_be_detection).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Gating : workspaceCountry !== 'BELGIQUE' → null (outil mono-pays BE).
 *
 * Logique miroir :
 *   `salaireMensuelBrutEur           ← aiData.salaireBrutMensuel > 0`
 *   `commissionParitaire             ← aiData.commissionParitaireBe`  // SF-246-23 BELGIQUE
 *   `joursTravaillesAnneePrecedente  ← aiData.joursTravaillesAnneePrecedenteBe`  // SF-246-23 BELGIQUE
 *   `joursPrestesEffectifs           ← aiData.joursPrestesBe`         // SF-246-23 BELGIQUE
 */

export interface AvantagesConventionnelsBePrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    /** SF-246-23 BELGIQUE — commission paritaire BE (≤ 20 car.). */
    commissionParitaireBe?: string | null;
    /** SF-246-23 BELGIQUE — jours travaillés année précédente [0, 365]. */
    joursTravaillesAnneePrecedenteBe?: number | null;
    /** SF-246-23 BELGIQUE — jours prestés depuis le 1er avril [0, 365]. */
    joursPrestesBe?: number | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: AvantagesConventionnelsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * SF-246-23 BELGIQUE — numéro/libellé de la commission paritaire applicable.
 * La string extraite par l'IA est normalisée vers un code whitelist CP_xxx.
 * Retourne le code normalisé si la correspondance est fiable, null sinon.
 * Concept distinct de conventionCollective (IDCC France).
 */
export function computeCommissionParitaire(input: AvantagesConventionnelsBePrefillInput): string | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const raw = input.aiData?.commissionParitaireBe;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  // Normalisation : tente de faire correspondre la string IA au code whitelist.
  // Patterns reconnus (case-insensitive, ignorer espaces/tirets) :
  const normalized = raw.toUpperCase().replace(/[\s\-\.]+/g, '_');
  if (normalized.includes('337')) return 'CP_337';
  if (normalized.includes('226')) return 'CP_226';
  if (normalized.includes('124')) return 'CP_124';
  if (normalized.includes('111')) return 'CP_111';
  // CP 200 / SCP 200.xx → CP_200 (la CP auxiliaire par défaut)
  if (normalized.includes('200')) return 'CP_200';
  return null; // Code non reconnu → pas de pré-fill (mieux vaut null que mauvaise valeur)
}

/**
 * SF-246-23 BELGIQUE — jours de travail effectif (ou assimilés) au cours de
 * l'année précédente. Base du calcul pécule vacances simple (Loi 28/06/1971).
 */
export function computeJoursTravailles(input: AvantagesConventionnelsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.joursTravaillesAnneePrecedenteBe;
  return typeof v === 'number' && v >= 0 && v <= 365 ? Math.round(v) : null;
}

/**
 * SF-246-23 BELGIQUE — jours effectivement prestés depuis le 1er avril de
 * l'exercice courant. Distinct des jours de l'année précédente.
 */
export function computeJoursPrestes(input: AvantagesConventionnelsBePrefillInput): number | null {
  if (input.workspaceCountry !== 'BELGIQUE') return null;
  const v = input.aiData?.joursPrestesBe;
  return typeof v === 'number' && v >= 0 && v <= 365 ? Math.round(v) : null;
}

export function computePrefillCount(input: AvantagesConventionnelsBePrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeCommissionParitaire(input) !== null) count++;   // SF-246-23
  if (computeJoursTravailles(input) !== null) count++;       // SF-246-23
  if (computeJoursPrestes(input) !== null) count++;          // SF-246-23
  return count;
}

export const AvantagesConventionnelsBeSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeCommissionParitaire,   // SF-246-23
  computeJoursTravailles,       // SF-246-23
  computeJoursPrestes,          // SF-246-23
  computePrefillCount,
};
