import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  VoieRegimeAlgerienCode,
  detectNationaliteAlgerienneFromIa,
  mapVoieFromIa,
} from '../../core/models/regime-algerien.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `RegimeAlgerienSectionComponent`
 * (F-IM-17-regime-algerien) — FR mono-pays.
 *
 * 2 champs : nationaliteAlgerienne (boolean detected) + voieDemande
 * (enum depuis typeProcedureDetectee → mapper).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RegimeAlgerienPrefillRules = {
  /** Heuristique défensive — souvent no-op (true OR null). */
  computeNationaliteAlgerienne(input: PrefillCountInput): true | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const detected = detectNationaliteAlgerienneFromIa(ai);
    return detected === true ? true : null;
  },

  computeVoieDemande(input: PrefillCountInput): VoieRegimeAlgerienCode | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapVoieFromIa(ai.typeProcedureDetectee);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeNationaliteAlgerienne(input) !== null) n++;
    if (this.computeVoieDemande(input) !== null) n++;
    return n;
  },
} as const;
