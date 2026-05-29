import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VoieNaturalisation } from '../../core/models/naturalisation-recours-tj.model';

/**
 * SF-214-30 — Helper partagé pour {@link NaturalisationRecoursTjSectionComponent}
 * (F-IM-39-naturalisation-recours-tj-fr) — FRANCE mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - voieNaturalisation : `aiData.naturalisationVoie` (whitelist
 *    MARIAGE / ASCENDANT / MINEUR_22_1).
 *  - dateRefusDeclaration : `aiData.naturalisationDateRefus` (ISO YYYY-MM-DD).
 *
 * Champ NON pré-rempli :
 *  - typeRefus : qualification juridique laissée à l'avocat (le pipeline IA
 *    ne distingue pas de façon fiable refus d'enregistrement vs contestation
 *    de nationalité depuis les pièces).
 *
 * Total : 2 champs pre-remplissables (sur 3 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const VOIE_VALUES: ReadonlySet<VoieNaturalisation> = new Set<VoieNaturalisation>([
  'MARIAGE',
  'ASCENDANT',
  'MINEUR_22_1',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const NaturalisationRecoursTjPrefillRules = {

  computeVoieNaturalisation(input: PrefillCountInput): VoieNaturalisation | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationVoie;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim() as VoieNaturalisation;
    return VOIE_VALUES.has(trimmed) ? trimmed : null;
  },

  computeDateRefusDeclaration(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationDateRefus;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeVoieNaturalisation(input) !== null) n++;
    if (this.computeDateRefusDeclaration(input) !== null) n++;
    return n;
  },
} as const;
