import { DublinRecoursPrefillRules as Rules } from './dublin-recours-section-prefill-rules';

describe('DublinRecoursPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2099-01-01' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '01/04/2026' } })).toBe(0);
  });

  it('returns 1 when dateNotificationDecisionContestee is valid', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
    })).toBe(1);
  });

  it('falls back to dateNotificationOqtf when dateNotificationDecisionContestee absent', () => {
    expect(Rules.computeDateNotificationDecisionTransfert({
      aiData: { dateNotificationOqtf: '2026-04-01' },
    })).toBe('2026-04-01');
  });
});
