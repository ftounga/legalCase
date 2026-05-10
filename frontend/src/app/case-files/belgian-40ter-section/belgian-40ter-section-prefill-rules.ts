import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { LIENS_FAMILIAUX, LienFamilial } from '../../core/models/belgian-40ter.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian40terSectionComponent`
 * (F-IM-14-40ter-familial-belge-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * 4 champs : lienFamilial (enum), regroupantBelge (boolean),
 * revenusMensuelsNetsEur (number > 0), dateDepotDemande (string non future).
 */

export const LIENS_FAMILIAUX_WHITELIST = new Set<LienFamilial>(
  LIENS_FAMILIAUX.map((l) => l.code),
);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian40terPrefillRules = {
  LIENS_FAMILIAUX_WHITELIST,

  computeLienFamilial(input: PrefillCountInput): LienFamilial | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const a = ai as Record<string, unknown>;
    const v = a['lienFamilialBe'] ?? a['lienFamilial'];
    if (typeof v !== 'string' || !LIENS_FAMILIAUX_WHITELIST.has(v as LienFamilial)) return null;
    return v as LienFamilial;
  },

  computeRegroupantBelge(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const a = ai as Record<string, unknown>;
    if (typeof a['regroupantBelge'] !== 'boolean') return null;
    return a['regroupantBelge'] as boolean;
  },

  computeRevenusMensuelsNets(input: PrefillCountInput): number | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const a = ai as Record<string, unknown>;
    const v = a['revenusNetsMensuels'] ?? a['revenusMensuelsNets'];
    if (typeof v !== 'number' || isNaN(v) || v <= 0) return null;
    return v;
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const a = ai as Record<string, unknown>;
    const v = a['dateDepotDemande'] ?? ai.dateDepotProcedure;
    if (typeof v !== 'string' || v.length === 0) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeLienFamilial(input) !== null) n++;
    if (this.computeRegroupantBelge(input) !== null) n++;
    if (this.computeRevenusMensuelsNets(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
