/** F-236 SF-236-02 — Tests `MediationFamilialePrefillRules`. */
import {
  MediationFamilialePrefillRules,
  computePrefillCount,
} from './mediation-familiale-section-prefill-rules';

describe('MediationFamilialePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: { motifSaisineMediationDetecte: '' } } as any)).toBe(0);
  });

  it('cas M — non applicable (binaire 0 ou 1)', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas N — motif présent', () => {
    expect(
      computePrefillCount({ aiData: { motifSaisineMediationDetecte: 'DIVORCE' } } as any),
    ).toBe(1);
  });

  it('surface', () => {
    expect(MediationFamilialePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
