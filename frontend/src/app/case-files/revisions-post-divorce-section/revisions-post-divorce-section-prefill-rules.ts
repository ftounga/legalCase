/**
 * F-236 SF-236-02 — Helper partagé `RevisionsPostDivorcePrefillRules`.
 *
 * 4 champs : revenusActuelsDebiteurEur, revenusActuelsCreancierEur,
 * nbEnfantsACharge, revenusAnnuelsEpoux (débiteur unifié SF-246-08).
 *
 * SF-246-08 : `nbEnfantsACharge` désormais champ réel de `FamilleExtractedData`
 * (sous-objet backend `vie_commune_detection`). Suppression de l'intersection
 * aspirationnelle. Ajout de `computeRevenusEpoux` lisant `revenusAnnuelsEpoux`
 * (nouveau champ réel unifié débiteur). Ajout de la garde BE : retourne 0 hors France.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface RevisionsPostDivorcePrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeRevenusDebiteur(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux1Eur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeRevenusCreancier(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux2Eur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeNbEnfants(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.nbEnfantsACharge;
  return typeof v === 'number' && v >= 0 ? v : null;
}

/**
 * SF-246-08 : revenus annuels du débiteur/demandeur (€) — champ réel unifié.
 * Invariant §5.2 (> 0 ou null). Priorité sur `revenusAnnuelsEpoux1Eur`
 * (rétro-compat divorce-accepte) quand les deux sont présents.
 */
export function computeRevenusEpoux(input: RevisionsPostDivorcePrefillInput): number | null {
  const v = input.aiData?.revenusAnnuelsEpoux;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: RevisionsPostDivorcePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
  let n = 0;
  if (computeRevenusDebiteur(input) !== null) n++;
  if (computeRevenusCreancier(input) !== null) n++;
  if (computeNbEnfants(input) !== null) n++;
  if (computeRevenusEpoux(input) !== null) n++;
  return n;
}

export const RevisionsPostDivorcePrefillRules = {
  computeRevenusDebiteur,
  computeRevenusCreancier,
  computeNbEnfants,
  computeRevenusEpoux,
  computePrefillCount,
};
