import { AesFamillePrefillRules as Rules } from './aes-famille-section-prefill-rules';

describe('AesFamillePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE', () => {
    const input = {
      aiData: { dateEntreeFrance: '2020-01-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 when date is too short or non-string', () => {
    expect(Rules.computePrefillCount({ aiData: { dateEntreeFrance: '2020' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateEntreeFrance: 42 } })).toBe(0);
  });

  it('returns N=2 when valid date provided (dateEntreeFrance + dureePresenceMois computed)', () => {
    const input = { aiData: { dateEntreeFrance: '2020-01-15T00:00:00Z' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDateEntreeFrance(input)).toBe('2020-01-15');
    expect(Rules.computeDureePresenceMois(input)).toBeGreaterThan(0);
  });

  it('returns 2 plancher à 0 si date dans le futur (très improbable)', () => {
    // Date future renvoie 0 (Math.max(0, ...)) — toujours 2 fields posés.
    const input = { aiData: { dateEntreeFrance: '2099-01-01' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDureePresenceMois(input)).toBe(0);
  });
});
