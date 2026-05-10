/**
 * F-236 SF-236-02 — Tests `CommunauteUniversellePrefillRules`.
 */
import {
  CommunauteUniversellePrefillRules,
  computePrefillCount,
} from './communaute-universelle-section-prefill-rules';

describe('CommunauteUniversellePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas M — 2/4 (booléens)', () => {
    expect(
      computePrefillCount({
        aiData: { contratNotarieDetected: true, enfantsNonCommunsDetected: false },
      } as any),
    ).toBe(2);
  });

  it('cas M — valeur négative rejetée', () => {
    expect(
      computePrefillCount({ aiData: { valeurCommunauteEurDetectee: -1 } } as any),
    ).toBe(0);
  });

  it('cas N — 4/4', () => {
    expect(
      computePrefillCount({
        aiData: {
          contratNotarieDetected: true,
          enfantsNonCommunsDetected: false,
          clauseAttributionIntegraleDetected: true,
          valeurCommunauteEurDetectee: 200000,
        },
      } as any),
    ).toBe(4);
  });

  it('surface', () => {
    expect(CommunauteUniversellePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
