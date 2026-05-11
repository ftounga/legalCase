import { CrrvRefusVisaPrefillRules as Rules } from './crrv-refus-visa-section-prefill-rules';

describe('CrrvRefusVisaPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2099-01-01' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '01/04/2026' } })).toBe(0);
  });

  it('returns 1 when date present', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
    })).toBe(1);
  });

  it('computeDateNotificationRefus returns null when not FRANCE', () => {
    expect(Rules.computeDateNotificationRefus({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });
});
