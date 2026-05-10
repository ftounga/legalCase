import { AesMetiersTensionPrefillRules as Rules } from './aes-metiers-tension-section-prefill-rules';

describe('AesMetiersTensionPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: undefined })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure is missing or non-string', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: 42 } })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure is malformed', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026/01/01' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: 'not-a-date' } })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure is in the future', () => {
    expect(
      Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2099-12-31' } }),
    ).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  // ── Cas M (partiel) — N=1 outil mono-champ, donc M=1 = N. ─────────────
  it('returns 1 when only dateDepotProcedure is set (M=N=1, mono-champ)', () => {
    const input = {
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateDepotDemande(input)).toBe('2026-04-01');
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns N (max théo = 1) when full payload alimente', () => {
    const input = {
      aiData: { dateDepotProcedure: '2026-03-15' },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('defaults workspaceCountry to FRANCE when absent', () => {
    const input = { aiData: { dateDepotProcedure: '2026-03-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
