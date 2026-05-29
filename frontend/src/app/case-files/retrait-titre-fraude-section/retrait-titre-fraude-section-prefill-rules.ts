import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifRetraitTitreFraude } from '../../core/models/retrait-titre-fraude.model';

/**
 * SF-214-42 — Helper partagé pour {@link RetraitTitreFraudeSectionComponent}
 * (F-IM-45-retrait-titre-fraude-fr) — FRANCE mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateRetrait : `aiData.retraitTitreDateRetrait` (ISO YYYY-MM-DD) — date de la
 *    décision de retrait du titre, point de départ du délai de recours TA.
 *  - motifRetrait : `aiData.retraitTitreMotif` (enum MotifRetraitTitreFraude).
 *
 * Champs NON pré-remplis :
 *  - miseEnDemeurePrealable : élément procédural laissé à l'avocat — non
 *    factualisable de façon fiable depuis les pièces.
 *  - dateMiseEnDemeure : idem (optionnel).
 *
 * Total : 2 champs pre-remplissables (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const MOTIFS_VALIDES: readonly MotifRetraitTitreFraude[] = [
  'MARIAGE_GRIS',
  'FAUSSES_DECLARATIONS',
  'FRAUDE_DOCUMENTAIRE',
  'PERTE_CONDITIONS',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RetraitTitreFraudePrefillRules = {

  computeDateRetrait(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.retraitTitreDateRetrait;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computeMotifRetrait(input: PrefillCountInput): MotifRetraitTitreFraude | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.retraitTitreMotif;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim() as MotifRetraitTitreFraude;
    return MOTIFS_VALIDES.includes(trimmed) ? trimmed : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateRetrait(input) !== null) n++;
    if (this.computeMotifRetrait(input) !== null) n++;
    return n;
  },
} as const;
