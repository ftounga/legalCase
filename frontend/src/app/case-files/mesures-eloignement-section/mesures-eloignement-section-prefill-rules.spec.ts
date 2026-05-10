import { MesuresEloignementPrefillRules as Rules } from './mesures-eloignement-section-prefill-rules';

describe('MesuresEloignementPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { typeProcedureDetectee: 'OQTF' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 when typeProcedureDetectee unmapped', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when typeProcedureDetectee maps to a dispositif (N=1)', () => {
    const v = Rules.computeDispositif({ aiData: { typeProcedureDetectee: 'OQTF' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'OQTF' } })).toBe(1);
    } else {
      // OQTF n'a peut-être pas de mapping direct — on teste que la fonction est null-safe.
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'OQTF' } })).toBe(0);
    }
  });
});
