import { ReferesAdminPrefillRules as Rules } from './referes-admin-section-prefill-rules';

describe('ReferesAdminPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateNotificationDecisionContestee: '2026-01-15',
        typeRecoursCode: 'OQTF',
        transfertImminentDetected: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 for future date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2099-12-31' } })).toBe(0);
  });

  it('returns 0 when typeRecoursCode is unknown', () => {
    expect(Rules.computePrefillCount({ aiData: { typeRecoursCode: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only date is valid', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2026-01-15' } })).toBe(1);
  });

  it('returns 1 when only typeRecoursCode is OQTF (mapping)', () => {
    const input = { aiData: { typeRecoursCode: 'OQTF' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDecisionContestee(input)).toBe('OQTF');
  });

  it('returns 1 when transfertImminent=true', () => {
    const input = { aiData: { transfertImminentDetected: true } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computePreuvesUrgence(input)).toEqual(['TRANSFERT_IMMINENT']);
  });

  it('returns 0 when transfertImminent=false', () => {
    expect(Rules.computePrefillCount({ aiData: { transfertImminentDetected: false } })).toBe(0);
  });

  it('returns N=3 when all three sources alimente', () => {
    const input = {
      aiData: {
        dateNotificationDecisionContestee: '2026-01-15',
        typeRecoursCode: 'REFUS_TITRE',
        transfertImminentDetected: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
