import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian9bisSectionComponent`
 * (F-IM-14-9bis-humanitaire-be) — BE only.
 *
 * NOTE F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' non
 * appliqué ici (runtime ne le fait pas non plus — anomalie (E)).
 *
 * 1 champ : dateDepotDemande (depuis aiData.dateDepotProcedure).
 */

export const Belgian9bisPrefillRules = {
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateDepotProcedure;
    if (typeof d !== 'string' || d.length === 0) return null;
    return d;
  },

  computePrefillCount(input: PrefillCountInput): number {
    return this.computeDateDepotDemande(input) !== null ? 1 : 0;
  },
} as const;
