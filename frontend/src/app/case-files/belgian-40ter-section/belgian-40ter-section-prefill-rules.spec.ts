import { Belgian40terPrefillRules as Rules, LIENS_FAMILIAUX_WHITELIST } from './belgian-40ter-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('Belgian40terPrefillRules', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 when revenusMensuelsNets <= 0 or NaN (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { revenusNetsMensuels: 0 } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { revenusNetsMensuels: -100 } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { revenusNetsMensuels: NaN } })).toBe(0);
  });

  it('returns 0 for future dateDepotDemande (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotDemande: '2099-12-31' } })).toBe(0);
  });

  it('returns 1 when only lienFamilial valide (alias lienFamilialBe) (BE)', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    expect(Rules.computePrefillCount({ ...BE, aiData: { lienFamilialBe: v } })).toBe(1);
  });

  it('returns 1 when only regroupantBelge boolean is set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { regroupantBelge: false } })).toBe(1);
  });

  it('returns 1 when only revenus > 0 (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { revenusNetsMensuels: 1500 } })).toBe(1);
  });

  it('returns 1 when only dateDepotProcedure fallback (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-01-01' } })).toBe(1);
  });

  it('returns N=4 when all four sources alimente (BELGIQUE)', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    const input = {
      ...BE,
      aiData: {
        lienFamilial: v,
        regroupantBelge: true,
        revenusMensuelsNets: 2000,
        dateDepotDemande: '2026-01-01',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    const input = {
      aiData: {
        lienFamilial: v,
        regroupantBelge: true,
        revenusMensuelsNets: 2000,
        dateDepotDemande: '2026-01-01',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeLienFamilial(input)).toBeNull();
    expect(Rules.computeRegroupantBelge(input)).toBeNull();
    expect(Rules.computeRevenusMensuelsNets(input)).toBeNull();
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    const v = [...LIENS_FAMILIAUX_WHITELIST][0];
    expect(Rules.computePrefillCount({ aiData: { lienFamilial: v } })).toBe(0);
  });
});
