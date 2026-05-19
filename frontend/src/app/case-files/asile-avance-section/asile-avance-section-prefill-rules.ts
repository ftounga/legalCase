import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DispositifAsileCode, mapDispositifFromIa } from '../../core/models/asile-avance.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `AsileAvanceSectionComponent`
 * (F-IM-12-asile-avance) — FR mono-pays.
 *
 * SF-246-19 : 2 champs pré-remplis désormais :
 *   1. `dispositifAsile` (depuis aiData.typeProcedureDetectee → mapper).
 *   2. `dateDecisionAnterieure` (YYYY-MM-DD non-future) — depuis `aiData.asileDateDecisionAnterieure`.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export const AsileAvancePrefillRules = {
  computeDispositifAsile(input: PrefillCountInput): DispositifAsileCode | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapDispositifFromIa(ai.typeProcedureDetectee ?? null);
  },

  /**
   * SF-246-19 : date de la décision antérieure sur demande d'asile.
   * Règles : ISO YYYY-MM-DD strict, non future, non null.
   */
  computeDateDecisionAnterieure(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.asileDateDecisionAnterieure;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDispositifAsile(input) !== null) n++;
    if (this.computeDateDecisionAnterieure(input) !== null) n++;
    return n;
  },
} as const;
