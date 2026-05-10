import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian40bisSectionComponent`
 * (F-IM-14-40bis-cohabitant-ue-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * 2 champs : dateDepotDemande (depuis aiData.dateDepotProcedure),
 * regroupantCitoyenUe (depuis aiData.nationaliteUe — best-effort).
 */

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian40bisPrefillRules = {
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateDepotProcedure;
    if (typeof d !== 'string' || d.length === 0) return null;
    return d;
  },

  /**
   * Best-effort : si l'IA détecte un client UE, on suppose le regroupant
   * UE aussi (l'avocat ajustera). Posé strictement si nationaliteUe est
   * un boolean.
   */
  computeRegroupantCitoyenUe(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.nationaliteUe !== 'boolean') return null;
    return ai.nationaliteUe;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeDateDepotDemande(input) !== null) n++;
    if (this.computeRegroupantCitoyenUe(input) !== null) n++;
    return n;
  },
} as const;
