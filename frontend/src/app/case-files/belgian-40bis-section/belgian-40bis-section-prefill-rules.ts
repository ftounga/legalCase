import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian40bisSectionComponent`
 * (F-IM-14-40bis-cohabitant-ue-be) — BE only.
 *
 * NOTE F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' non
 * appliqué ici (anomalie (E) — rattrapage SF-236-04).
 *
 * 2 champs : dateDepotDemande (depuis aiData.dateDepotProcedure),
 * regroupantCitoyenUe (depuis aiData.nationaliteUe — best-effort).
 */

export const Belgian40bisPrefillRules = {
  computeDateDepotDemande(input: PrefillCountInput): string | null {
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
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.nationaliteUe !== 'boolean') return null;
    return ai.nationaliteUe;
  },

  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateDepotDemande(input) !== null) n++;
    if (this.computeRegroupantCitoyenUe(input) !== null) n++;
    return n;
  },
} as const;
