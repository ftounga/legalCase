import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-38 — Helper partagé pour {@link ItfJudiciaireSectionComponent}
 * (F-IM-43-itf-judiciaire-fr) — FRANCE mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateCondamnation : `aiData.itfJudiciaireDateCondamnation`
 *    (ISO YYYY-MM-DD) — date de la condamnation pénale prononçant l'ITF, point
 *    de départ des délais de recours.
 *  - dureeITFAnnees : `aiData.itfJudiciaireDureeAnnees` (entier > 0) — durée de
 *    l'interdiction du territoire français fixée par le juge pénal.
 *
 * Champs NON pré-remplis :
 *  - infractionPrincipale : qualification de l'infraction (texte libre) laissée
 *    à l'avocat — non factualisable de façon fiable depuis les pièces.
 *  - condamnationDefinitive : appréciation juridique (la condamnation est-elle
 *    devenue définitive ?) laissée à l'avocat.
 *  - dateEcheanceRecoursPenal : date d'échéance optionnelle laissée à l'avocat.
 *
 * Total : 2 champs pre-remplissables (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const ItfJudiciairePrefillRules = {

  computeDateCondamnation(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.itfJudiciaireDateCondamnation;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computeDureeAnnees(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.itfJudiciaireDureeAnnees;
    if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateCondamnation(input) !== null) n++;
    if (this.computeDureeAnnees(input) !== null) n++;
    return n;
  },
} as const;
