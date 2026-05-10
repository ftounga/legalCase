import { Belgian9bisPrefillRules as Rules } from './belgian-9bis-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('Belgian9bisPrefillRules', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure missing or non-string (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: null } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '' } })).toBe(0);
  });

  it('returns 1 when dateDepotProcedure is set in BE (M=N=1)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('computeDateDepotDemande returns string when set in BE', () => {
    expect(Rules.computeDateDepotDemande({ ...BE, aiData: { dateDepotProcedure: '2026-03-15' } })).toBe('2026-03-15');
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = { aiData: { dateDepotProcedure: '2026-04-01' }, workspaceCountry: 'FRANCE' };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    const input = { aiData: { dateDepotProcedure: '2026-04-01' } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
