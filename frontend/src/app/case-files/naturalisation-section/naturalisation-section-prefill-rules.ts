import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `NaturalisationSectionComponent`
 * (F-IM-13-naturalisation) — FR mono-pays.
 *
 * SF-246-19 : 3 champs pré-remplis désormais branchés :
 *   1. `dureeResidenceReguliereAnnees` (number) — depuis `aiData.natDureeResidenceReguliereAnnees`.
 *   2. `dureeMariageAnnees` (number) — depuis `aiData.natDureeMariageAnnees`.
 *   3. `ageDemandeur` (number) — depuis `aiData.natAgeDemandeur`.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function boundedInt(v: unknown, min: number, max: number): number | null {
  if (typeof v !== 'number' || !Number.isInteger(v) || v < min || v > max) return null;
  return v;
}

export const NaturalisationPrefillRules = {
  /** Durée résidence régulière en années (0–70). */
  computeDureeResidenceReguliereAnnees(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.natDureeResidenceReguliereAnnees, 0, 70);
  },

  /** Durée de mariage en années (0–70). */
  computeDureeMariageAnnees(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.natDureeMariageAnnees, 0, 70);
  },

  /** Âge du demandeur en années (0–120). */
  computeAgeDemandeur(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.natAgeDemandeur, 0, 120);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDureeResidenceReguliereAnnees(input) !== null) n++;
    if (this.computeDureeMariageAnnees(input) !== null) n++;
    if (this.computeAgeDemandeur(input) !== null) n++;
    return n;
  },
} as const;
