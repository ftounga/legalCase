/** F-236 SF-236-02 — Tests `OrdonnanceRequetePrefillRules`. */
import {
  OrdonnanceRequetePrefillRules,
  computePrefillCount,
} from './ordonnance-requete-section-prefill-rules';

describe('OrdonnanceRequetePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — presenceEnfants false ne compte pas', () => {
    expect(computePrefillCount({ aiData: { presenceEnfantsDetected: false } } as any)).toBe(0);
  });

  it('cas N — presenceEnfants true', () => {
    expect(computePrefillCount({ aiData: { presenceEnfantsDetected: true } } as any)).toBe(1);
  });

  it('surface', () => {
    expect(OrdonnanceRequetePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
