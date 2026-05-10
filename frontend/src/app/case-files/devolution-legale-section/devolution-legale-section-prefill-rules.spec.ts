/** F-236 SF-236-02 — Tests `DevolutionLegalePrefillRules`. */
import {
  DevolutionLegalePrefillRules,
  computePrefillCount,
} from './devolution-legale-section-prefill-rules';

describe('DevolutionLegalePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/4', () => {
    expect(
      computePrefillCount({
        aiData: { conjointSurvivantDetected: true, nbDescendantsDetecte: 2 },
      } as any),
    ).toBe(2);
  });

  it('rejette nb négatifs', () => {
    expect(
      computePrefillCount({ aiData: { nbDescendantsDetecte: -1 } } as any),
    ).toBe(0);
  });

  it('cas N — 4/4', () => {
    expect(
      computePrefillCount({
        aiData: {
          conjointSurvivantDetected: true,
          nbDescendantsDetecte: 2,
          tousDescendantsCommunsAvecConjointDetected: true,
          nbFreresSoeursDetecte: 1,
        },
      } as any),
    ).toBe(4);
  });

  it('surface', () => {
    expect(DevolutionLegalePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
