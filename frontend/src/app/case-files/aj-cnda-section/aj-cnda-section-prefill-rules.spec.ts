import { AjCndaPrefillRules as Rules } from './aj-cnda-section-prefill-rules';

describe('AjCndaPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { asileDateDecisionAnterieure: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateDecisionOFPRA(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when asileDateDecisionAnterieure is a valid ISO date', () => {
    const input = { aiData: { asileDateDecisionAnterieure: '2026-01-15' } };
    expect(Rules.computeDateDecisionOFPRA(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateDecisionOFPRA({ aiData: { asileDateDecisionAnterieure: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateDecisionOFPRA({ aiData: { asileDateDecisionAnterieure: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateDecisionOFPRA({ aiData: { asileDateDecisionAnterieure: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateDecisionOFPRA({
      aiData: { asileDateDecisionAnterieure: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateDecisionOFPRA({ aiData: { asileDateDecisionAnterieure: null } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { asileDateDecisionAnterieure: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
