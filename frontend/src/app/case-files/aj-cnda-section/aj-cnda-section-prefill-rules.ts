import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-20 — Helper partagé pour {@link AjCndaSectionComponent}
 * (F-IM-34-aj-cnda-fr) — FRANCE UNIQUEMENT.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateDecisionOFPRA : `aiData.asileDateDecisionAnterieure` (ISO YYYY-MM-DD).
 *
 * Champs NON pré-remplis :
 *  - ressourcesMensuellesNettes / procedureAcceleree / demandeAJDeposee /
 *    dateDepotAJ : pas de champ IA factualisable dédié, saisis par l'avocat.
 *
 * Total : 1 champ pre-remplissable (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AjCndaPrefillRules = {

  computeDateDecisionOFPRA(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.asileDateDecisionAnterieure;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateDecisionOFPRA(input) !== null) n++;
    return n;
  },
} as const;
