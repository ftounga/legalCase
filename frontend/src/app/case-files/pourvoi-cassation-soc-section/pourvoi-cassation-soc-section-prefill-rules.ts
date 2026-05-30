import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-06 — Helper partagé pour {@link PourvoiCassationSocSectionComponent}
 * (F-DT-87-pourvoi-cassation-soc) — Travail FR mono-pays.
 *
 * Champ pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateNotificationArret : `aiData.dateNotificationArretAppel` (ISO YYYY-MM-DD).
 *    Point de départ du délai de pourvoi de 2 mois (art. 612 CPC).
 *
 * Champs NON pré-remplis :
 *  - casOuverture : qualification juridique (les 7 cas d'ouverture) laissée à
 *    l'avocat — non factualisable de façon fiable depuis les pièces.
 *  - representationAvocatCassation / moyenSerieuxIdentifie : appréciations
 *    procédurales laissées à l'avocat.
 *  - `pourvoiCassationSocEnvisage` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pre-remplissable (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const PourvoiCassationSocPrefillRules = {

  computeDateNotificationArret(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateNotificationArretAppel;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationArret(input) !== null) n++;
    return n;
  },
} as const;
