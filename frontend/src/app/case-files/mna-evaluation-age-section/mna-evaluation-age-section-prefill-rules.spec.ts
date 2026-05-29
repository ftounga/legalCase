import { MnaEvaluationAgePrefillRules as Rules } from './mna-evaluation-age-section-prefill-rules';

describe('MnaEvaluationAgePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { mineursDateNaissance: '2010-06-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNaissanceDeclaree(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when mineursDateNaissance is a valid ISO date', () => {
    const input = { aiData: { mineursDateNaissance: '2010-06-15' } };
    expect(Rules.computeDateNaissanceDeclaree(input)).toBe('2010-06-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateNaissanceDeclaree({ aiData: { mineursDateNaissance: '  2009-02-01  ' } }))
      .toBe('2009-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateNaissanceDeclaree({ aiData: { mineursDateNaissance: '15/06/2010' } })).toBeNull();
    expect(Rules.computeDateNaissanceDeclaree({ aiData: { mineursDateNaissance: '2010-6-5' } })).toBeNull();
    expect(Rules.computeDateNaissanceDeclaree({
      aiData: { mineursDateNaissance: 20100615 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateNaissanceDeclaree({ aiData: { mineursDateNaissance: null } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { mineursDateNaissance: '2011-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
