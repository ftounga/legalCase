import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ImmigrationRecoursSectionComponent` (F-IM-06-recours).
 *
 * 2 champs : `recoursType` (enum) + `dateNotification` (ISO).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_RECOURS_CODES = new Set([
  'RECOURS_GRACIEUX_PREFET', 'RECOURS_CONTENTIEUX_TA', 'RECOURS_CNDA',
  'RECOURS_CGRA', 'RECOURS_CCE', 'RECOURS_CE_BELGIQUE',
]);

export const ImmigrationRecoursPrefillRules = {
  ISO_DATE_RE,
  VALID_RECOURS_CODES,

  computeRecoursType(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const code = typeof ai.typeRecoursCode === 'string'
      ? ai.typeRecoursCode.toUpperCase()
      : null;
    if (!code || !VALID_RECOURS_CODES.has(code)) return null;
    return code;
  },

  computeDateNotification(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const date = ai.dateNotificationDecisionContestee;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    return date;
  },

  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRecoursType(input) !== null) n++;
    if (this.computeDateNotification(input) !== null) n++;
    return n;
  },
} as const;
