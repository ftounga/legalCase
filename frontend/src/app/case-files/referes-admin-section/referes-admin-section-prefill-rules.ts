import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DecisionContestee, PreuveUrgence } from '../../core/models/referes-admin.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `ReferesAdminSectionComponent`
 * (F-IM-08-referes-admin-fr) — FR mono-pays.
 *
 * 3 champs : dateNotificationDecision (ISO non future),
 * decisionContestee (enum via mapping typeRecoursCode),
 * preuvesUrgence (array ['TRANSFERT_IMMINENT'] si détecté).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const TYPE_RECOURS_TO_DECISION: Readonly<Record<string, DecisionContestee>> = {
  OQTF: 'OQTF',
  REFUS_TITRE: 'REFUS_TITRE',
  RETRAIT_TITRE: 'RETRAIT_TITRE',
  IRTF: 'IRTF',
};

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const ReferesAdminPrefillRules = {
  ISO_DATE_RE,
  TYPE_RECOURS_TO_DECISION,

  computeDateNotificationDecision(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = ai.dateNotificationDecisionContestee;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    if (date > todayIso()) return null;
    return date;
  },

  computeDecisionContestee(input: PrefillCountInput): DecisionContestee | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const code = ai.typeRecoursCode;
    if (!code || !TYPE_RECOURS_TO_DECISION[code]) return null;
    return TYPE_RECOURS_TO_DECISION[code];
  },

  computePreuvesUrgence(input: PrefillCountInput): PreuveUrgence[] | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return ai.transfertImminentDetected === true
      ? (['TRANSFERT_IMMINENT'] as PreuveUrgence[])
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationDecision(input) !== null) n++;
    if (this.computeDecisionContestee(input) !== null) n++;
    if (this.computePreuvesUrgence(input) !== null) n++;
    return n;
  },
} as const;
