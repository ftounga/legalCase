/**
 * F-236 SF-236-02 — Tests `PartageJudiciairePrefillRules`.
 */
import {
  PartageJudiciairePrefillRules,
  computePrefillCount,
} from './partage-judiciaire-section-prefill-rules';

describe('PartageJudiciairePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — pv + tentative seulement', () => {
    expect(
      computePrefillCount({
        aiData: { pvDifficultesEtablisDetected: true, tentativeAmiableEpuiseueeDetected: false },
      } as any),
    ).toBe(2);
  });

  it('rejette nombreCoindivisaires < 2', () => {
    expect(
      computePrefillCount({ aiData: { nombreCoindivisairesDetecte: 1 } } as any),
    ).toBe(0);
  });

  it('cas N — 4/4', () => {
    expect(
      computePrefillCount({
        aiData: {
          pvDifficultesEtablisDetected: true,
          tentativeAmiableEpuiseueeDetected: true,
          nombreCoindivisairesDetecte: 3,
          valeurBiensIndivisionEur: 500000,
        },
      } as any),
    ).toBe(4);
  });

  it('surface', () => {
    expect(PartageJudiciairePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
