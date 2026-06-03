import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifDecheance } from '../../core/models/decheance-nationalite.model';

/**
 * SF-220-05 — Helper partagé pour {@link DecheanceNationaliteSectionComponent}
 * (F-IM-51-decheance-nationalite-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - motif : `aiData.decheanceMotif` (whitelist 4 codes).
 *  - binational : `aiData.decheanceBinational` (booléen).
 *  - mesurePrononcee : `aiData.decheanceMesurePrononcee` (booléen).
 *  - dateDecret : `aiData.decheanceDateDecret` (chaîne ISO yyyy-MM-dd).
 *
 * Champs NON pré-remplis (non extraits par le pipeline IA — non factualisables
 * de façon fiable depuis les pièces, saisie avocat) : dateAcquisitionNationalite,
 * dateFaits.
 *
 * Le flag `decheanceNationaliteDetectee` est le pivot de VISIBILITÉ (CONTEXTUAL),
 * pas un champ saisissable du formulaire — il ne compte pas dans le pré-fill.
 *
 * Total : 4 champs pre-remplissables.
 */

const MOTIF_CODES: ReadonlySet<string> = new Set<string>([
  'TERRORISME',
  'ATTEINTE_INTERETS_NATION',
  'FRAUDE_ACQUISITION',
  'AUTRE',
]);

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const DecheanceNationalitePrefillRules = {

  computeMotif(input: PrefillCountInput): MotifDecheance | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.decheanceMotif;
    if (typeof v !== 'string' || !MOTIF_CODES.has(v)) return null;
    return v as MotifDecheance;
  },

  computeBinational(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.decheanceBinational;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeMesurePrononcee(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.decheanceMesurePrononcee;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeDateDecret(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.decheanceDateDecret;
    if (typeof v !== 'string' || !ISO_DATE.test(v)) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeMotif(input) !== null) n++;
    if (this.computeBinational(input) !== null) n++;
    if (this.computeMesurePrononcee(input) !== null) n++;
    if (this.computeDateDecret(input) !== null) n++;
    return n;
  },
} as const;
