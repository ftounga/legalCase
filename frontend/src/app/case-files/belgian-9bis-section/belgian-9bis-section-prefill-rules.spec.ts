import { Belgian9bisPrefillRules as Rules } from './belgian-9bis-section-prefill-rules';

describe('Belgian9bisPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure missing or non-string', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '' } })).toBe(0);
  });

  it('returns 1 when dateDepotProcedure is set (M=N=1)', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 nominal — string non vide', () => {
    expect(Rules.computeDateDepotDemande({ aiData: { dateDepotProcedure: '2026-03-15' } })).toBe('2026-03-15');
  });
});
