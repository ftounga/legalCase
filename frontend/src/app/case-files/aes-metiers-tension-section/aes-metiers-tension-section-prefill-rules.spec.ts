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

  // ── Cas partiel — dateDepotDemande seul ───────────────────────────────
  it('returns 1 when only dateDepotProcedure is set', () => {
    const input = {
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateDepotDemande(input)).toBe('2026-04-01');
  });

  it('defaults workspaceCountry to FRANCE when absent', () => {
    const input = { aiData: { dateDepotProcedure: '2026-03-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  // ── SF-246-18 : computeDateEntreeFrance ───────────────────────────────
  it('computeDateEntreeFrance returns null when absent', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: {} })).toBeNull();
  });

  it('computeDateEntreeFrance returns null for future date', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2099-01-01' } })).toBeNull();
  });

  it('computeDateEntreeFrance returns date for valid past ISO', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2020-06-01' } })).toBe('2020-06-01');
  });

  it('computeDateEntreeFrance returns null for BELGIQUE', () => {
    expect(Rules.computeDateEntreeFrance({
      aiData: { aesDateEntreeFrance: '2020-06-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── SF-246-18 : computeMoisActiviteSalariee ───────────────────────────
  it('computeMoisActiviteSalariee returns null when absent', () => {
    expect(Rules.computeMoisActiviteSalariee({ aiData: {} })).toBeNull();
  });

  it('computeMoisActiviteSalariee returns null for > 24', () => {
    expect(Rules.computeMoisActiviteSalariee({ aiData: { aesMoisActiviteSalariee: 25 } })).toBeNull();
  });

  it('computeMoisActiviteSalariee returns value for 0–24', () => {
    expect(Rules.computeMoisActiviteSalariee({ aiData: { aesMoisActiviteSalariee: 18 } })).toBe(18);
    expect(Rules.computeMoisActiviteSalariee({ aiData: { aesMoisActiviteSalariee: 0 } })).toBe(0);
    expect(Rules.computeMoisActiviteSalariee({ aiData: { aesMoisActiviteSalariee: 24 } })).toBe(24);
  });

  it('computeMoisActiviteSalariee returns null for BELGIQUE', () => {
    expect(Rules.computeMoisActiviteSalariee({
      aiData: { aesMoisActiviteSalariee: 18 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── SF-246-18 : computeCodeMetier ─────────────────────────────────────
  it('computeCodeMetier returns null when absent', () => {
    expect(Rules.computeCodeMetier({ aiData: {} })).toBeNull();
  });

  it('computeCodeMetier returns null for empty string', () => {
    expect(Rules.computeCodeMetier({ aiData: { aesCodeMetier: '' } })).toBeNull();
    expect(Rules.computeCodeMetier({ aiData: { aesCodeMetier: '   ' } })).toBeNull();
  });

  it('computeCodeMetier returns trimmed string for valid code', () => {
    expect(Rules.computeCodeMetier({ aiData: { aesCodeMetier: 'N1101' } })).toBe('N1101');
    expect(Rules.computeCodeMetier({ aiData: { aesCodeMetier: '  Infirmier(ère)  ' } })).toBe('Infirmier(ère)');
  });

  it('computeCodeMetier returns null for BELGIQUE', () => {
    expect(Rules.computeCodeMetier({
      aiData: { aesCodeMetier: 'N1101' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computePrefillCount — cas maximal ────────────────────────────────
  it('returns 4 when all 4 fields present', () => {
    const input = {
      aiData: {
        dateDepotProcedure: '2026-04-01',
        aesDateEntreeFrance: '2020-06-01',
        aesMoisActiviteSalariee: 18,
        aesCodeMetier: 'N1101',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('returns 0 for BELGIQUE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        dateDepotProcedure: '2026-04-01',
        aesDateEntreeFrance: '2020-06-01',
        aesMoisActiviteSalariee: 18,
        aesCodeMetier: 'N1101',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
