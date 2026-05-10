import { Belgian40terPrefillRules as Rules, LIENS_FAMILIAUX_WHITELIST } from './belgian-40ter-section-prefill-rules';

describe('Belgian40terPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when revenusMensuelsNets <= 0 or NaN', () => {
    expect(Rules.computePrefillCount({ aiData: { revenusNetsMensuels: 0 } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { revenusNetsMensuels: -100 } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { revenusNetsMensuels: NaN } })).toBe(0);
  });

  it('returns 0 for future dateDepotDemande', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotDemande: '2099-12-31' } })).toBe(0);
  });

  it('returns 1 when only lienFamilial valide (alias lienFamilialBe)', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    expect(Rules.computePrefillCount({ aiData: { lienFamilialBe: v } })).toBe(1);
  });

  it('returns 1 when only regroupantBelge boolean is set', () => {
    expect(Rules.computePrefillCount({ aiData: { regroupantBelge: false } })).toBe(1);
  });

  it('returns 1 when only revenus > 0', () => {
    expect(Rules.computePrefillCount({ aiData: { revenusNetsMensuels: 1500 } })).toBe(1);
  });

  it('returns 1 when only dateDepotProcedure fallback', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-01-01' } })).toBe(1);
  });

  it('returns N=4 when all four sources alimente', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    const input = {
      aiData: {
        lienFamilial: v,
        regroupantBelge: true,
        revenusMensuelsNets: 2000,
        dateDepotDemande: '2026-01-01',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
