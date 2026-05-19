import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  DispositifEloignementCode,
  MotifMenaceCode,
  mapDispositifFromIa,
  mapMotifMenaceFromIa,
} from '../../core/models/mesures-eloignement.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `MesuresEloignementSectionComponent`
 * (F-IM-20-mesures-eloignement) — FR mono-pays.
 *
 * SF-246-19 : 3 champs pré-remplis désormais :
 *   1. `dispositif` (depuis aiData.typeProcedureDetectee → mapper).
 *   2. `dureePresenceIrreguliereMois` (number 0–600) — depuis `aiData.eloiDureePresenceIrreguliereMois`.
 *   3. `motifMenace` (MotifMenaceCode) — depuis `aiData.eloiMotifMenace` via `mapMotifMenaceFromIa`.
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

  /**
   * SF-246-19 : durée de présence irrégulière en France en mois (0–600).
   */
  computeDureePresenceIrreguliereMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.eloiDureePresenceIrreguliereMois;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 600) return null;
    return v;
  },

  /**
   * SF-246-19 : motif de menace depuis `aiData.eloiMotifMenace` via
   * `mapMotifMenaceFromIa` (whitelist 5 codes, insensible à la casse).
   */
  computeMotifMenace(input: PrefillCountInput): MotifMenaceCode | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapMotifMenaceFromIa(ai.eloiMotifMenace ?? null);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDispositif(input) !== null) n++;
    if (this.computeDureePresenceIrreguliereMois(input) !== null) n++;
    if (this.computeMotifMenace(input) !== null) n++;
    return n;
  },
} as const;
