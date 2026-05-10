import {
  ImmigrationRecoursPrefillRules as Rules,
  VALID_RECOURS_CODES,
} from './immigration-recours-section-prefill-rules';

describe('ImmigrationRecoursPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when typeRecoursCode is unknown', () => {
    expect(Rules.computePrefillCount({ aiData: { typeRecoursCode: 'BOGUS' } })).toBe(0);
  });

  it('returns 0 when date is malformed', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: 'not-a-date' } })).toBe(0);
  });

  it('returns 1 when only valid recoursType is present', () => {
    const input = { aiData: { typeRecoursCode: 'recours_gracieux_prefet' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRecoursType(input)).toBe('RECOURS_GRACIEUX_PREFET');
  });

  it('returns 1 when only valid date is present', () => {
    const input = { aiData: { dateNotificationDecisionContestee: '2026-01-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateNotification(input)).toBe('2026-01-15');
  });

  it('returns N=2 when both fields are present', () => {
    const input = {
      aiData: {
        typeRecoursCode: 'RECOURS_CONTENTIEUX_TA',
        dateNotificationDecisionContestee: '2026-03-01',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('accepts every valid code', () => {
    for (const c of VALID_RECOURS_CODES) {
      expect(Rules.computeRecoursType({ aiData: { typeRecoursCode: c } })).toBe(c);
    }
  });
});
