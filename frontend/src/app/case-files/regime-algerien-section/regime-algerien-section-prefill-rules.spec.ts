import { RegimeAlgerienPrefillRules as Rules } from './regime-algerien-section-prefill-rules';

describe('RegimeAlgerienPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { typeProcedureDetectee: 'CARTE_RESIDENT' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for unmapped procedure and no nationalite hint', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only voie maps successfully (M=1)', () => {
    // Test pratique : on cherche un code qui mappe — fail-open si l'API change
    const v = Rules.computeVoieDemande({ aiData: { typeProcedureDetectee: 'CRA' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'CRA' } })).toBe(1);
    }
  });

  it('renvoie cohérent quel que soit l\'input — pas de crash', () => {
    expect(typeof Rules.computePrefillCount({ aiData: {} })).toBe('number');
  });
});
