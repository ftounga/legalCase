/** F-236 SF-236-02 — Tests `SeparationCorpsPrefillRules`. */
import {
  SeparationCorpsPrefillRules,
  computePrefillCount,
} from './separation-corps-section-prefill-rules';

describe('SeparationCorpsPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — date seule', () => {
    expect(
      computePrefillCount({ aiData: { dateSeparation: '2024-06-15' } }),
    ).toBe(1);
  });

  it('cas N — 2/2', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-06-15', patrimoineCommun: true },
      } as any),
    ).toBe(2);
  });

  it('surface', () => {
    expect(SeparationCorpsPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
