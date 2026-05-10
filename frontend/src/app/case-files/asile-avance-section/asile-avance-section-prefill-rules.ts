import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DispositifAsileCode, mapDispositifFromIa } from '../../core/models/asile-avance.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `AsileAvanceSectionComponent`
 * (F-IM-12-asile-avance) — FR mono-pays.
 *
 * 1 champ : dispositifAsile (depuis aiData.typeProcedureDetectee → mapper).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AsileAvancePrefillRules = {
  computeDispositifAsile(input: PrefillCountInput): DispositifAsileCode | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapDispositifFromIa(ai.typeProcedureDetectee ?? null);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    return this.computeDispositifAsile(input) !== null ? 1 : 0;
  },
} as const;
