import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-02 — Helper partagé pour {@link AppelCphSectionComponent}
 * (F-DT-86-appel-cph-cour-appel) — FRANCE mono-pays.
 *
 * Champ pré-rempli (depuis {@link TravailExtractedData}) :
 *  - dateNotificationJugement : `aiData.dateNotificationJugement`
 *    (ISO YYYY-MM-DD) — date de notification du jugement du conseil de
 *    prud'hommes, point de départ du délai d'appel d'1 mois (art. 538 CPC).
 *
 * Champs NON pré-remplis (laissés à l'avocat — non factualisables de façon
 * fiable depuis les pièces) :
 *  - partieAppelante : qui forme l'appel (choix procédural de l'avocat).
 *  - modeNotification : signification par huissier vs LRAR (point de droit).
 *  - representationConstituee : représentation envisagée pour l'appel.
 *  - jugementEnDernierRessort : appréciation juridique du taux de ressort.
 *
 * Total : 1 champ pré-remplissable (sur 5 saisissables).
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
