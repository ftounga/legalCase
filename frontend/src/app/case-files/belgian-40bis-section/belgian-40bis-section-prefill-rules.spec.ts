import { Belgian40bisPrefillRules as Rules } from './belgian-40bis-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('Belgian40bisPrefillRules', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 1 when only dateDepotProcedure is set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 when only nationaliteUe is set (false posé) (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { nationaliteUe: false } })).toBe(1);
    expect(Rules.computeRegroupantCitoyenUe({ ...BE, aiData: { nationaliteUe: false } })).toBe(false);
  });

  it('returns N=2 when both fields are present (BELGIQUE)', () => {
    const input = {
      ...BE,
      aiData: {
        dateDepotProcedure: '2026-04-01',
        nationaliteUe: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 0 when nationaliteUe is not boolean (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { nationaliteUe: 'true' } })).toBe(0);
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      aiData: { dateDepotProcedure: '2026-04-01', nationaliteUe: true },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
    expect(Rules.computeRegroupantCitoyenUe(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(0);
  });
});
