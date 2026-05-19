import { AsileAvancePrefillRules as Rules } from './asile-avance-section-prefill-rules';

describe('AsileAvancePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { typeProcedureDetectee: 'ASILE_OFPRA', asileDateDecisionAnterieure: '2022-01-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for unmapped procedure', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('null-safe sur null typeProcedureDetectee', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: null } })).toBe(0);
  });

  it('returns 1 quand mapping dispositif non-null', () => {
    const v = Rules.computeDispositifAsile({ aiData: { typeProcedureDetectee: 'ASILE_OFPRA' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'ASILE_OFPRA' } })).toBe(1);
    }
  });

  // ── SF-246-19 : computeDateDecisionAnterieure ─────────────────────────
  it('computeDateDecisionAnterieure returns null when absent', () => {
    expect(Rules.computeDateDecisionAnterieure({ aiData: {} })).toBeNull();
  });

  it('computeDateDecisionAnterieure returns null for future date', () => {
    expect(Rules.computeDateDecisionAnterieure({ aiData: { asileDateDecisionAnterieure: '2099-01-01' } })).toBeNull();
  });

  it('computeDateDecisionAnterieure returns null for malformed date', () => {
    expect(Rules.computeDateDecisionAnterieure({ aiData: { asileDateDecisionAnterieure: '01/01/2022' } })).toBeNull();
    expect(Rules.computeDateDecisionAnterieure({ aiData: { asileDateDecisionAnterieure: 'not-a-date' } })).toBeNull();
  });

  it('computeDateDecisionAnterieure returns date for valid past ISO', () => {
    expect(Rules.computeDateDecisionAnterieure({ aiData: { asileDateDecisionAnterieure: '2022-06-15' } })).toBe('2022-06-15');
  });

  it('computeDateDecisionAnterieure returns null for BELGIQUE', () => {
    expect(Rules.computeDateDecisionAnterieure({
      aiData: { asileDateDecisionAnterieure: '2022-06-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computePrefillCount avec nouveau champ ────────────────────────────
  it('returns 1 when only dateDecisionAnterieure present', () => {
    expect(Rules.computePrefillCount({ aiData: { asileDateDecisionAnterieure: '2022-06-15' } })).toBe(1);
  });

  it('returns 2 when dispositif + dateDecisionAnterieure both present', () => {
    const v = Rules.computeDispositifAsile({ aiData: { typeProcedureDetectee: 'ASILE_OFPRA' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({
        aiData: {
          typeProcedureDetectee: 'ASILE_OFPRA',
          asileDateDecisionAnterieure: '2022-06-15',
        },
      })).toBe(2);
    }
  });
});
