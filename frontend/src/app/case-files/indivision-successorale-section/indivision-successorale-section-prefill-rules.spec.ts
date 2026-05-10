/** F-236 SF-236-02 — Tests `IndivisionSuccessoralePrefillRules`. */
import {
  IndivisionSuccessoralePrefillRules,
  computePrefillCount,
  parseTypeFromIa,
} from './indivision-successorale-section-prefill-rules';

describe('IndivisionSuccessoralePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('parseTypeFromIa', () => {
    expect(parseTypeFromIa('legale')).toBe('INDIVISION_LEGALE');
    expect(parseTypeFromIa('CONVENTION')).toBe('INDIVISION_CONVENTIONNELLE');
    expect(parseTypeFromIa('xx')).toBeNull();
  });

  it('cas N — 2/2', () => {
    expect(
      computePrefillCount({
        aiData: {
          typeIndivisionSuccessoraleDetecte: 'INDIVISION_LEGALE',
          dateOuvertureSuccessionDetectee: '2024-08-10',
        },
      } as any),
    ).toBe(2);
  });

  it('surface', () => {
    expect(IndivisionSuccessoralePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
