import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DispositifEloignementCode, mapDispositifFromIa } from '../../core/models/mesures-eloignement.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `MesuresEloignementSectionComponent`
 * (F-IM-20-mesures-eloignement) — FR mono-pays.
 *
 * 1 champ : dispositif (depuis aiData.typeProcedureDetectee → mapper).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const MesuresEloignementPrefillRules = {
  computeDispositif(input: PrefillCountInput): DispositifEloignementCode | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapDispositifFromIa(ai.typeProcedureDetectee);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    return this.computeDispositif(input) !== null ? 1 : 0;
  },
} as const;
