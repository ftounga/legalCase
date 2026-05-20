/**
 * F-237 SF-237-02 — Helper partagé `TransactionPrefillRules` (module pur).
 *
 * Extrait la logique de pré-fill IA du composant `TransactionSectionComponent`
 * (F-DT-31, FR uniquement, art. 2044 Cciv) selon le contrat F-236 SF-236-01.
 *
 * 3 champs pré-fillables :
 *   1. salaireBrutMensuel (number > 0)
 *   2. ancienneteAnnees (years, dérivée de dateEntree + dateLicenciement/now)
 *   3. rupturePrealable (RupturePrealable enum, mappé depuis motifLicenciement)
 *
 * Le module est volontairement pur :
 *  - n'importe rien d'Angular ;
 *  - aucune dépendance temporelle implicite hors `new Date()` qui sert
 *    uniquement quand `dateLicenciement` est absent (fallback : aujourd'hui)
 *    — point partagé strictement entre le static et le runtime.
 */
import { RupturePrealable } from '../../core/models/transaction.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Mapping strict (clé `toUpperCase`) `motifLicenciement` IA → enum
 * `RupturePrealable`. Tolérant aux variantes via keywords (cf. `computeRupture`).
 */
export const AI_MOTIF_TO_RUPTURE_PREALABLE: Readonly<Record<string, RupturePrealable>> = {
  LICENCIEMENT_PERSONNEL: 'LICENCIEMENT_PERSONNEL',
  LICENCIEMENT_ECONOMIQUE: 'LICENCIEMENT_ECONOMIQUE',
  RUPTURE_CONVENTIONNELLE: 'RUPTURE_CONVENTIONNELLE',
  DEMISSION: 'DEMISSION',
  FIN_CDD: 'FIN_CDD',
};

export interface TransactionPrefillInput {
  aiData?: TravailExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/**
 * Salaire brut mensuel — valeur IA directement reportable.
 * Retourne `null` si manquant / non-numérique / ≤ 0.
 */
export function computeSalaire(input: TransactionPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * Ancienneté en années entières (arrondi inférieur) calculée depuis
 * `dateEntree` et `dateLicenciement` (ou aujourd'hui par défaut).
 *
 * Retourne `null` si :
 *  - `dateEntree` absente ou non-ISO,
 *  - `dateLicenciement` présente mais non-ISO,
 *  - `entree > ref` (ordre inversé),
 *  - calcul invalide (NaN).
 */
export function computeAnciennete(input: TransactionPrefillInput): number | null {
  const ai = input.aiData;
  if (!ai) return null;
  const dateEntree = ai.dateEntree;
  if (typeof dateEntree !== 'string' || !ISO_DATE_REGEX.test(dateEntree)) return null;
  const refDateStr = typeof ai.dateLicenciement === 'string' && ISO_DATE_REGEX.test(ai.dateLicenciement)
    ? ai.dateLicenciement
    : null;
  const entree = new Date(dateEntree);
  if (isNaN(entree.getTime())) return null;
  const ref = refDateStr ? new Date(refDateStr) : new Date();
  if (refDateStr && isNaN(ref.getTime())) return null;
  if (entree > ref) return null;
  const diffMs = ref.getTime() - entree.getTime();
  const years = Math.floor(diffMs / (365.25 * 24 * 60 * 60 * 1000));
  return years >= 0 ? years : null;
}

/**
 * Rupture préalable mappée depuis la chaîne libre IA `motifLicenciement`.
 *
 * Heuristique :
 *  1. match strict sur l'enum (case-insensible),
 *  2. keywords français usuels (économique, faute/personnel, conventionnelle,
 *     démission, cdd/fin de contrat).
 *
 * Retourne `null` si aucun mapping ne s'applique.
 */
export function computeRupture(input: TransactionPrefillInput): RupturePrealable | null {
  const raw = input.aiData?.motifLicenciement;
  if (!raw || typeof raw !== 'string') return null;
  const upper = raw.trim().toUpperCase();
  if (!upper) return null;
  if (AI_MOTIF_TO_RUPTURE_PREALABLE[upper]) return AI_MOTIF_TO_RUPTURE_PREALABLE[upper];
  const lower = raw.toLowerCase();
  if (lower.includes('économique') || lower.includes('economique')) return 'LICENCIEMENT_ECONOMIQUE';
  if (lower.includes('personnel') || lower.includes('faute')) return 'LICENCIEMENT_PERSONNEL';
  if (lower.includes('conventionnelle')) return 'RUPTURE_CONVENTIONNELLE';
  if (lower.includes('démission') || lower.includes('demission')) return 'DEMISSION';
  if (lower.includes('cdd') || lower.includes('fin de contrat')) return 'FIN_CDD';
  return null;
}

/**
 * SF-246-21 : date de signature du protocole transactionnel (ISO).
 * Distincte de dateLicenciement et de dateMiseEnDemeure.
 */
export function computeDateSignature(input: TransactionPrefillInput): string | null {
  const v = input.aiData?.transactionDateSignature;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/**
 * SF-246-21 : montant de l'indemnité transactionnelle (€ > 0).
 */
export function computeIndemniteTransactionnelle(input: TransactionPrefillInput): number | null {
  const v = input.aiData?.transactionIndemniteMontantEur;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * Maître — compte les champs non-null. Strict miroir du runtime
 * `prefillFromAi()` de `TransactionSectionComponent`.
 */
export function computePrefillCount(input: TransactionPrefillInput): number {
  let n = 0;
  if (computeSalaire(input) !== null) n++;
  if (computeAnciennete(input) !== null) n++;
  if (computeRupture(input) !== null) n++;
  if (computeDateSignature(input) !== null) n++;
  if (computeIndemniteTransactionnelle(input) !== null) n++;
  return n;
}

export const TransactionPrefillRules = {
  ISO_DATE_REGEX,
  AI_MOTIF_TO_RUPTURE_PREALABLE,
  computeSalaire,
  computeAnciennete,
  computeRupture,
  computeDateSignature,
  computeIndemniteTransactionnelle,
  computePrefillCount,
};
