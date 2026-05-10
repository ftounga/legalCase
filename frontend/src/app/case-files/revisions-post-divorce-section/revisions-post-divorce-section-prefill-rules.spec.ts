/**
 * F-236 SF-236-02 — Tests `RevisionsPostDivorcePrefillRules`.
 */
import {
  RevisionsPostDivorcePrefillRules,
  computePrefillCount,
} from './revisions-post-divorce-section-prefill-rules';

describe('RevisionsPostDivorcePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas M — revenus seul (strict >0)', () => {
    expect(
      computePrefillCount({ aiData: { revenusAnnuelsEpoux1Eur: 45000 } }),
    ).toBe(1);
    expect(
      computePrefillCount({ aiData: { revenusAnnuelsEpoux1Eur: 0 } }),
    ).toBe(0);
  });

  it('cas N — 3/3 champs', () => {
    expect(
      computePrefillCount({
        aiData: {
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 30000,
          nbEnfantsACharge: 2,
        },
      }),
    ).toBe(3);
  });

  it('nbEnfants 0 compte (valide)', () => {
    expect(computePrefillCount({ aiData: { nbEnfantsACharge: 0 } })).toBe(1);
  });

  it('surface', () => {
    expect(RevisionsPostDivorcePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
