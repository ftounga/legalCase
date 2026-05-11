/**
 * F-237 SF-237-02 — Tests `TravailProcedurePrefillRules`.
 */
import {
  TravailProcedurePrefillRules,
  computePrefillCount,
  computeTypeProcedure,
  computeDateDeclencheur,
} from './travail-procedure-section-prefill-rules';

describe('TravailProcedurePrefillRules', () => {
  it('cas 0 — retourne 0 quand vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computePrefillCount({ aiData: {} as any })).toBe(0);
  });

  it('cas M — partiel (date seule = 1/2, hors gating pays)', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { dateDeclencheurProcedure: '2026-04-01' } as any,
      }),
    ).toBe(1);
  });

  it('cas N — nominal 2/2 (FR + date)', () => {
    expect(
      computePrefillCount({
        aiData: {
          procedureTravailDetectee: 'PRUDHOMMES_FR',
          dateDeclencheurProcedure: '2026-04-01',
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any,
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  // ── computeTypeProcedure — gating pays ─────────────────────────────────
  it('computeTypeProcedure — code FR + workspace FR → match', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' } as any,
        workspaceCountry: 'FRANCE',
      }),
    ).toBe('PRUDHOMMES_FR');
  });

  it('computeTypeProcedure — code BE + workspace BE → match', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE' } as any,
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe('TRIBUNAL_TRAVAIL_BE');
  });

  it('computeTypeProcedure — code FR + workspace BE → null (gating)', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' } as any,
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — code BE + workspace FR → null (gating)', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE' } as any,
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — code inconnu → null', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'PAS_DANS_LE_SET_FR' } as any,
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — default workspaceCountry = FRANCE', () => {
    expect(
      computeTypeProcedure({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' } as any,
      }),
    ).toBe('PRUDHOMMES_FR');
  });

  // ── computeDateDeclencheur ─────────────────────────────────────────────
  it('computeDateDeclencheur — rejette non-ISO', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: '01/04/2026' } as any }),
    ).toBeNull();
  });

  it('computeDateDeclencheur — accepte ISO', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: '2026-04-01' } as any }),
    ).toBe('2026-04-01');
  });

  it('expose surface complète', () => {
    expect(TravailProcedurePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
