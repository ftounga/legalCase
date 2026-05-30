import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-02 — Helper partagé pour {@link AppelCphSectionComponent}
 * (F-DT-86-appel-cph-cour-appel) — Travail FR mono-pays.
 *
 * Champ pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateNotificationJugement : `aiData.dateNotificationJugement` (ISO YYYY-MM-DD).
 *    Point de départ du délai d'appel d'un mois (art. 538 CPC ; R. 1461-1 CPC).
 *
 * Champs NON pré-remplis :
 *  - partieAppelante / modeNotification / representationConstituee /
 *    jugementEnDernierRessort : pas de champ IA dédié, saisis par l'avocat.
 *  - `appelCphEnvisage` est un FLAG de visibilité (déclenche l'apparition de
 *    l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pre-remplissable (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AppelCphPrefillRules = {

  computeDateNotificationJugement(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateNotificationJugement;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationJugement(input) !== null) n++;
    return n;
  },
} as const;
