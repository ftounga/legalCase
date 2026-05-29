import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-22 — Helper partagé pour {@link VictimeTraiteSectionComponent}
 * (F-IM-35-victime-traite-l4251-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - plainteDeposee : `aiData.tehPlainteDeposee` (boolean).
 *  - datePlainte : `aiData.tehDatePlainte` (ISO yyyy-MM-dd).
 *
 * Champs NON pré-remplis :
 *  - collaborationOCRTEH / presenceAutoriteRefugieDetectee : pas de champ IA
 *    dédié, saisis par l'avocat.
 *
 * Total : 2 champs pre-remplissables.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VictimeTraitePrefillRules = {
  ISO_DATE_RE,

  computePlainteDeposee(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const val = (ai as { tehPlainteDeposee?: unknown }).tehPlainteDeposee;
    if (typeof val !== 'boolean') return null;
    return val;
  },

  computeDatePlainte(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = (ai as { tehDatePlainte?: unknown }).tehDatePlainte;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    return date;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computePlainteDeposee(input) !== null) n++;
    if (this.computeDatePlainte(input) !== null) n++;
    return n;
  },
} as const;
