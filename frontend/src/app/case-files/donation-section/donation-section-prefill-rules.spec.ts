/** F-236 SF-236-02 — Tests `DonationPrefillRules`. */
import {
  DonationPrefillRules,
  computePrefillCount,
  parseFormeFromIa,
} from './donation-section-prefill-rules';

describe('DonationPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('parseFormeFromIa', () => {
    expect(parseFormeFromIa('NOTARIEE')).toBe('DONATION_NOTARIEE');
    expect(parseFormeFromIa('DEGUISE')).toBe('DONATION_DEGUISEE');
    expect(parseFormeFromIa('BIZARRE')).toBeNull();
  });

  it('cas N — 4/4', () => {
    expect(
      computePrefillCount({
        aiData: {
          formeDonationDetectee: 'NOTARIEE',
          dateDonationDetectee: '2020-09-12',
          saineDEspritDonateurDetected: true,
          respectQuotiteDisponibleDetected: true,
        },
      } as any),
    ).toBe(4);
  });

  it('surface', () => {
    expect(DonationPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
