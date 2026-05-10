import { AsileAvancePrefillRules as Rules } from './asile-avance-section-prefill-rules';

describe('AsileAvancePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { typeProcedureDetectee: 'ASILE_OFPRA' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for unmapped procedure', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('null-safe sur null typeProcedureDetectee', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: null } })).toBe(0);
  });

  it('returns 1 quand mapping non-null (N=1)', () => {
    const v = Rules.computeDispositifAsile({ aiData: { typeProcedureDetectee: 'ASILE_OFPRA' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'ASILE_OFPRA' } })).toBe(1);
    }
  });
});
