import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-08 — Helper partagé pour {@link VlsTsValidationSectionComponent}
 * (F-IM-28-vls-ts-validation-ofii-fr) — FR mono-pays.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateEntreeFrance : `aiData.aesDateEntreeFrance` (ISO YYYY-MM-DD).
 *
 * Champs NON pré-remplis :
 *  - typeVlsTs / validationOFIIEffectuee / dateValidationOFII :
 *    pas de champ IA dédié, saisis par l'avocat.
 *
 * Total : 1 champ pre-remplissable (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VlsTsValidationPrefillRules = {

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDateEntreeFrance;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    return n;
  },
} as const;
