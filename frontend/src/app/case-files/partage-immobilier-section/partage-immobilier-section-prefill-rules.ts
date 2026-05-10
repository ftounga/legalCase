/**
 * F-236 SF-236-02 — Helper partagé `PartageImmobilierPrefillRules` (module pur).
 *
 * Consommé par :
 * - `prefillFromAi()` runtime du composant
 * - `static getPrefillCount` du composant
 *
 * Stricte parité runtime/static garantie par construction.
 */
import { BienItem, LiquidationCommunaute } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const IMMO_KEYWORDS = [
  'immobilier', 'maison', 'appartement', 'résidence', 'residence', 'villa',
  'studio', 'terrain', 'logement',
];

export const PRET_KEYWORDS = [
  'prêt', 'pret', 'emprunt', 'crédit', 'credit', 'hypothèque', 'hypotheque',
  'hypothécaire', 'hypothecaire',
];

export function matchesKeyword(libelle: string | null | undefined, keywords: string[]): boolean {
  if (!libelle) return false;
  const lower = libelle.toLowerCase();
  return keywords.some(k => lower.includes(k));
}

export interface PartageImmobilierPrefillInput {
  aiData?: FamilleExtractedData | null;
  liquidationCommunaute?: LiquidationCommunaute | null;
  synthesis?: { liquidationCommunaute?: LiquidationCommunaute | null } | null;
  // Champs ignorés (cohérence du contrat unifié).
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

function resolveLiquidation(input: PartageImmobilierPrefillInput): LiquidationCommunaute | null | undefined {
  return input.liquidationCommunaute ?? input.synthesis?.liquidationCommunaute;
}

/**
 * Valeur vénale pré-remplie depuis aiData (priorité) ou liquidation (fallback
 * mono-bien). Retourne null si aucune source valide.
 */
export function computeValeurImmeuble(input: PartageImmobilierPrefillInput): number | null {
  const ai = input.aiData;
  if (typeof ai?.valeurImmeuble === 'number' && ai.valeurImmeuble > 0) {
    return ai.valeurImmeuble;
  }
  const liq = resolveLiquidation(input);
  const biens = (liq?.actifCommun ?? []).filter(
    (b: BienItem) =>
      matchesKeyword(b.libelle, IMMO_KEYWORDS) &&
      typeof b.valeur === 'number' && (b.valeur as number) > 0,
  );
  return biens.length === 1 ? (biens[0].valeur as number) : null;
}

/**
 * Capital restant dû pré-rempli depuis aiData (priorité) ou liquidation
 * (fallback mono-prêt). Retourne null si aucune source valide.
 */
export function computeCapitalRestantDu(input: PartageImmobilierPrefillInput): number | null {
  const ai = input.aiData;
  if (typeof ai?.capitalRestantDu === 'number' && ai.capitalRestantDu >= 0) {
    return ai.capitalRestantDu;
  }
  const liq = resolveLiquidation(input);
  const prets = (liq?.passifCommun ?? []).filter(
    (p: BienItem) =>
      matchesKeyword(p.libelle, PRET_KEYWORDS) &&
      typeof p.valeur === 'number' && (p.valeur as number) >= 0,
  );
  return prets.length === 1 ? (prets[0].valeur as number) : null;
}

export function computePrefillCount(input: PartageImmobilierPrefillInput): number {
  let n = 0;
  if (computeValeurImmeuble(input) !== null) n++;
  if (computeCapitalRestantDu(input) !== null) n++;
  return n;
}

export const PartageImmobilierPrefillRules = {
  IMMO_KEYWORDS,
  PRET_KEYWORDS,
  matchesKeyword,
  computeValeurImmeuble,
  computeCapitalRestantDu,
  computePrefillCount,
};
