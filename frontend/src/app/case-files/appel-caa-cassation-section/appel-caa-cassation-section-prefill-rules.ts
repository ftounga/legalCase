import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-34 — Helper partagé pour {@link AppelCaaCassationSectionComponent}
 * (F-IM-41-appel-caa-cassation-ce-fr) — FRANCE mono-pays.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateJugementTA : `aiData.recoursDateJugementTA` (ISO YYYY-MM-DD) — date du
 *    jugement du tribunal administratif qui constitue le point de départ du délai
 *    d'appel devant la CAA.
 *
 * Champs NON pré-remplis :
 *  - typeDecisionTA : qualification juridique (rejet vs annulation) laissée à
 *    l'avocat — non factualisable de façon fiable depuis les pièces.
 *  - typeContentieux : nature du contentieux (OQTF / refus de titre / expulsion /
 *    autre) laissée à l'avocat.
 *  - delaiSpecialOQTF : décision procédurale (application du délai spécial 15 j)
 *    laissée à l'avocat.
 *
 * Total : 1 champ pre-remplissable (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AppelCaaCassationPrefillRules = {

  computeDateJugementTA(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.recoursDateJugementTA;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateJugementTA(input) !== null) n++;
    return n;
  },
} as const;
