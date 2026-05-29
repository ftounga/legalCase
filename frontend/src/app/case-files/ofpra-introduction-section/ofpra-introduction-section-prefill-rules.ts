import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-18 — Helper partagé pour {@link OfpraIntroductionSectionComponent}
 * (F-IM-33-ofpra-introduction-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateArriveeEnFrance : `aiData.aesDateEntreeFrance` (ISO yyyy-MM-dd).
 *
 * Champs NON pré-remplis :
 *  - passageGudaEffectue : saisi par l'avocat (checkbox).
 *  - datePassageGuda : saisi par l'avocat depuis l'attestation GUDA.
 *  - adaRequise : saisi par l'avocat (checkbox).
 *
 * Total : 1 champ pre-remplissable (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const OfpraIntroductionPrefillRules = {
  ISO_DATE_RE,

  computeDateArriveeEnFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = (ai as { aesDateEntreeFrance?: unknown }).aesDateEntreeFrance;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    return date;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateArriveeEnFrance(input) !== null) n++;
    return n;
  },
} as const;
