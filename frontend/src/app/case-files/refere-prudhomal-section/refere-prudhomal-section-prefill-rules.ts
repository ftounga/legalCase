/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Référé prud'homal" (FR).
 * SF-246-21 : branchement d'1 champ depuis `procedure_details_detection`
 * (refereMontantProvision — € > 0).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `dateMiseEnDemeure     ← aiData.dateLicenciement` (ISO, <= today)
 *   `ancienneteContratMois ← monthsBetween(dateEntree, dateLicenciement || today)`
 *   `natureCreance         ← HEURES_SUPPLEMENTAIRES` si IA détecte heures sup > 0
 *   `montantProvision      ← aiData.refereMontantProvision` (SF-246-21, € > 0)
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface RefereProhomalPrefillInput {
  aiData?: {
    dateLicenciement?: string | null;
    dateEntree?: string | null;
    heuresSupMentionneesDansDossier?: {
      totalDeclarees25pct?: number | null;
      totalDeclarees50pct?: number | null;
      horsContingent?: number | null;
    } | null;
    // SF-246-21 — procedure_details_detection
    refereMontantProvision?: number | null;
  } | null;
  workspaceCountry?: string;
  /** Date "now" ISO injectable pour les tests (YYYY-MM-DD). */
  todayIso?: string;
}

function todayOf(input: RefereProhomalPrefillInput): string {
  return input.todayIso ?? new Date().toISOString().slice(0, 10);
}

export function computeDateMiseEnDemeure(input: RefereProhomalPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
  return v <= todayOf(input) ? v : null;
}

export function monthsBetween(startIso: string, endIso: string): number | null {
  if (!ISO_DATE_RE.test(startIso) || !ISO_DATE_RE.test(endIso)) return null;
  const start = new Date(startIso);
  const end = new Date(endIso);
  if (isNaN(start.getTime()) || isNaN(end.getTime())) return null;
  if (end < start) return 0;
  const years = end.getFullYear() - start.getFullYear();
  const months = end.getMonth() - start.getMonth();
  let total = years * 12 + months;
  if (end.getDate() < start.getDate()) total -= 1;
  return Math.max(total, 0);
}

export function computeAncienneteContratMois(input: RefereProhomalPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const dateEntree = input.aiData?.dateEntree;
  if (typeof dateEntree !== 'string' || !ISO_DATE_RE.test(dateEntree)) return null;
  const dateLic = input.aiData?.dateLicenciement;
  const endRef = (typeof dateLic === 'string' && ISO_DATE_RE.test(dateLic)) ? dateLic : todayOf(input);
  const mois = monthsBetween(dateEntree, endRef);
  if (mois === null || mois < 0) return null;
  return mois;
}

export function computeNatureCreance(input: RefereProhomalPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const h = input.aiData?.heuresSupMentionneesDansDossier;
  if (!h) return null;
  const hasHeuresSup =
    (typeof h.totalDeclarees25pct === 'number' && h.totalDeclarees25pct > 0) ||
    (typeof h.totalDeclarees50pct === 'number' && h.totalDeclarees50pct > 0) ||
    (typeof h.horsContingent === 'number' && h.horsContingent > 0);
  return hasHeuresSup ? 'HEURES_SUPPLEMENTAIRES' : null;
}

/** SF-246-21 : montant de la provision demandée en référé (€ > 0). */
export function computeMontantProvision(input: RefereProhomalPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.refereMontantProvision;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: RefereProhomalPrefillInput): number {
  let count = 0;
  if (computeDateMiseEnDemeure(input) !== null) count++;
  if (computeAncienneteContratMois(input) !== null) count++;
  if (computeNatureCreance(input) !== null) count++;
  if (computeMontantProvision(input) !== null) count++;
  return count;
}

export const ReferePrudhomalSectionPrefillRules = {
  computeDateMiseEnDemeure,
  computeAncienneteContratMois,
  computeNatureCreance,
  computeMontantProvision,
  computePrefillCount,
  monthsBetween,
};
