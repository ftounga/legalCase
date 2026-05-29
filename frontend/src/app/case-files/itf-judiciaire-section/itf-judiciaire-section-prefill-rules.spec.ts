import { ItfJudiciairePrefillRules as Rules } from './itf-judiciaire-section-prefill-rules';

describe('ItfJudiciairePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { itfJudiciaireDateCondamnation: '2026-01-15', itfJudiciaireDureeAnnees: 5 },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateCondamnation(input)).toBeNull();
    expect(Rules.computeDureeAnnees(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only date is valid (FRANCE)', () => {
    const input = { aiData: { itfJudiciaireDateCondamnation: '2026-01-15' } };
    expect(Rules.computeDateCondamnation(input)).toBe('2026-01-15');
    expect(Rules.computeDureeAnnees(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 when only duree is valid (FRANCE)', () => {
    const input = { aiData: { itfJudiciaireDureeAnnees: 5 } };
    expect(Rules.computeDureeAnnees(input)).toBe(5);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 when both date and duree are valid (FRANCE)', () => {
    const input = { aiData: { itfJudiciaireDateCondamnation: '2026-01-15', itfJudiciaireDureeAnnees: 5 } };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateCondamnation({ aiData: { itfJudiciaireDateCondamnation: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateCondamnation({ aiData: { itfJudiciaireDateCondamnation: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateCondamnation({ aiData: { itfJudiciaireDateCondamnation: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateCondamnation({
      aiData: { itfJudiciaireDateCondamnation: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateCondamnation({ aiData: { itfJudiciaireDateCondamnation: null } })).toBeNull();
  });

  it('rejects non-number / non-positive / non-finite duree', () => {
    expect(Rules.computeDureeAnnees({ aiData: { itfJudiciaireDureeAnnees: 0 } })).toBeNull();
    expect(Rules.computeDureeAnnees({ aiData: { itfJudiciaireDureeAnnees: -3 } })).toBeNull();
    expect(Rules.computeDureeAnnees({ aiData: { itfJudiciaireDureeAnnees: '5' as unknown as number } })).toBeNull();
    expect(Rules.computeDureeAnnees({ aiData: { itfJudiciaireDureeAnnees: NaN } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    expect(Rules.computePrefillCount({ aiData: { itfJudiciaireDateCondamnation: '2026-03-10', itfJudiciaireDureeAnnees: 2 } })).toBe(2);
  });
});
