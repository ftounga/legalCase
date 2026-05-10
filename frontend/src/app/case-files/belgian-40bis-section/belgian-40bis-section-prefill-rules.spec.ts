import { Belgian40bisPrefillRules as Rules } from './belgian-40bis-section-prefill-rules';

describe('Belgian40bisPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 1 when only dateDepotProcedure is set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 when only nationaliteUe is set (false posé)', () => {
    expect(Rules.computePrefillCount({ aiData: { nationaliteUe: false } })).toBe(1);
    expect(Rules.computeRegroupantCitoyenUe({ aiData: { nationaliteUe: false } })).toBe(false);
  });

  it('returns N=2 when both fields are present', () => {
    const input = {
      aiData: {
        dateDepotProcedure: '2026-04-01',
        nationaliteUe: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 0 when nationaliteUe is not boolean', () => {
    expect(Rules.computePrefillCount({ aiData: { nationaliteUe: 'true' } })).toBe(0);
  });
});
