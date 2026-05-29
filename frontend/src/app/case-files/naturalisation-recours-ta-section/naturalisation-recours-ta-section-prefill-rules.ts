import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-32 — Helper partagé pour {@link NaturalisationRecoursTaSectionComponent}
 * (F-IM-40-naturalisation-recours-ta-fr) — FRANCE mono-pays.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateRefusDecret : `aiData.naturalisationDateRefus` (ISO YYYY-MM-DD) —
 *    réutilise le champ partagé introduit en SF-214-29 / SF-214-30.
 *
 * Champs NON pré-remplis :
 *  - motivationRefus : texte libre repris des pièces, laissé à l'avocat (pas de
 *    champ structuré fiable côté pipeline IA).
 *  - recoursPrerequis : information procédurale (recours administratif préalable
 *    effectué) non factualisable depuis les pièces.
 *
 * Total : 1 champ pre-remplissable (sur 3 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const NaturalisationRecoursTaPrefillRules = {

  computeDateRefusDecret(input: PrefillCountInput): string | null {
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
    if (this.computeDateRefusDecret(input) !== null) n++;
    return n;
  },
} as const;
