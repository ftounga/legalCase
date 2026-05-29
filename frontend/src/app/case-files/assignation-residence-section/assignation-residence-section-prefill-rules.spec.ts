import { AssignationResidencePrefillRules as Rules } from './assignation-residence-section-prefill-rules';

describe('AssignationResidencePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { assignationDateNotification: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotification(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when date is valid (FRANCE)', () => {
    const input = { aiData: { assignationDateNotification: '2026-01-15' } };
    expect(Rules.computeDateNotification(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateNotification({ aiData: { assignationDateNotification: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
    expect(Rules.computePrefillCount({ aiData: { assignationDateNotification: '  2026-02-01  ' } })).toBe(1);
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateNotification({ aiData: { assignationDateNotification: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateNotification({ aiData: { assignationDateNotification: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateNotification({
      aiData: { assignationDateNotification: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateNotification({ aiData: { assignationDateNotification: null } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    expect(Rules.computePrefillCount({ aiData: { assignationDateNotification: '2026-03-10' } })).toBe(1);
  });
});
