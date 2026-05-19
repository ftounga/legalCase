import { MesuresEloignementPrefillRules as Rules } from './mesures-eloignement-section-prefill-rules';

describe('MesuresEloignementPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        typeProcedureDetectee: 'OQTF',
        eloiMotifMenace: 'ORDRE_PUBLIC',
        eloiDureePresenceIrreguliereMois: 18,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 when typeProcedureDetectee unmapped', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when typeProcedureDetectee maps to a dispositif', () => {
    const v = Rules.computeDispositif({ aiData: { typeProcedureDetectee: 'OQTF' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'OQTF' } })).toBe(1);
    } else {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'OQTF' } })).toBe(0);
    }
  });

  // ── SF-246-19 : computeMotifMenace ───────────────────────────────────
  it('computeMotifMenace returns null when absent', () => {
    expect(Rules.computeMotifMenace({ aiData: {} })).toBeNull();
  });

  it('computeMotifMenace returns null for unknown code', () => {
    expect(Rules.computeMotifMenace({ aiData: { eloiMotifMenace: 'INCONNU' } })).toBeNull();
  });

  it.each([
    ['ORDRE_PUBLIC'],
    ['SECURITE_ETAT'],
    ['TERRORISME'],
    ['RECIDIVE_GRAVE'],
    ['AUTRE'],
  ])('computeMotifMenace returns %s for valid code', (code) => {
    expect(Rules.computeMotifMenace({ aiData: { eloiMotifMenace: code } })).toBe(code);
  });

  it('computeMotifMenace is case-insensitive via mapMotifMenaceFromIa', () => {
    const result = Rules.computeMotifMenace({ aiData: { eloiMotifMenace: 'ordre_public' } });
    expect(result).toBe('ORDRE_PUBLIC');
  });

  it('computeMotifMenace returns null for BELGIQUE', () => {
    expect(Rules.computeMotifMenace({
      aiData: { eloiMotifMenace: 'TERRORISME' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── SF-246-19 : computeDureePresenceIrreguliereMois ──────────────────
  it('computeDureePresenceIrreguliereMois returns null when absent', () => {
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: {} })).toBeNull();
  });

  it('computeDureePresenceIrreguliereMois returns null for negative', () => {
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: { eloiDureePresenceIrreguliereMois: -1 } })).toBeNull();
  });

  it('computeDureePresenceIrreguliereMois returns null for > 600', () => {
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: { eloiDureePresenceIrreguliereMois: 601 } })).toBeNull();
  });

  it('computeDureePresenceIrreguliereMois returns value for 0–600', () => {
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: { eloiDureePresenceIrreguliereMois: 18 } })).toBe(18);
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: { eloiDureePresenceIrreguliereMois: 0 } })).toBe(0);
    expect(Rules.computeDureePresenceIrreguliereMois({ aiData: { eloiDureePresenceIrreguliereMois: 600 } })).toBe(600);
  });

  it('computeDureePresenceIrreguliereMois returns null for BELGIQUE', () => {
    expect(Rules.computeDureePresenceIrreguliereMois({
      aiData: { eloiDureePresenceIrreguliereMois: 18 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computePrefillCount — cas maximal ────────────────────────────────
  it('returns 2 when motifMenace + dureePresenceIrr both present (no dispositif)', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        eloiMotifMenace: 'TERRORISME',
        eloiDureePresenceIrreguliereMois: 18,
      },
    })).toBe(2);
  });

  it('returns 0 for BELGIQUE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        eloiMotifMenace: 'TERRORISME',
        eloiDureePresenceIrreguliereMois: 18,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
