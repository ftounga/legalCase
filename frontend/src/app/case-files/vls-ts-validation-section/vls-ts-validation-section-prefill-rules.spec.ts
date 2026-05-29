import { VlsTsValidationPrefillRules as Rules } from './vls-ts-validation-section-prefill-rules';

describe('VlsTsValidationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { aesDateEntreeFrance: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateEntreeFrance(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when aesDateEntreeFrance is a valid ISO date', () => {
    const input = { aiData: { aesDateEntreeFrance: '2026-01-15' } };
    expect(Rules.computeDateEntreeFrance(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateEntreeFrance({
      aiData: { aesDateEntreeFrance: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: null } })).toBeNull();
  });

  it('does NOT count vlsTsValidationOFIIEffectuee alone (not a prefilled field)', () => {
    const input = { aiData: { vlsTsValidationOFIIEffectuee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { aesDateEntreeFrance: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
